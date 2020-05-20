package com.diafyt.lazarus.utils

import android.nfc.Tag
import android.util.Log
import kotlin.math.min

/**
 * Organize the deployment of data onto a tag.
 */
class DeliveryPlan(val sections: List<DeliverySection>) {
    /**
     * Describe the data to be delivered by specifying the position of
     * the initial block to be written as well as the data to be written.
     */
    class DeliverySection(val initialBlock: Byte, val data: ByteArray)

    /**
     * Execute the actual delivery as described by this instance.
     */
    suspend fun deliver(tag: Tag): Boolean {
        Log.i(javaClass.name, "Payload delivery starting.")
        for (section in sections) {
            var offset = 0
            while (offset < section.data.size) {
                val num = min(2, (section.data.size - offset) / 8)
                val pos = (section.initialBlock + (offset / 8)).toByte()
                val blocks = section.data.sliceArray(offset until (offset + 8*num))
                Log.d(javaClass.name, "Going to write $num blocks at $pos")
                val answer = if (num == 1) {
                    NFCUtil.writeBlock(tag, pos, blocks)
                } else {
                    NFCUtil.writeMultipleBlocks(tag, pos, blocks)
                }
                if (answer == null) {
                    Log.i(javaClass.name, "Payload delivery error.")
                    return false
                }
                offset += 8*num
            }
        }
        Log.i(javaClass.name, "Payload delivery complete.")
        return true
    }

    companion object {
        private const val libreBaseAddress = 0xf860

        /**
         * Verify that the plan has the desired properties.
         *
         * This only checks things not checked in the parse() method.
         */
        private fun verify(plan: DeliveryPlan): DeliveryPlan? {
            var programKey: Long? = null
            for (section in plan.sections) {
                val endBlock = section.initialBlock + section.data.size / 8
                if (section.initialBlock <= 39 && endBlock >= 39) {
                    programKey = Util.littleEndianDecode(
                        section.data.sliceArray(
                            (0x13c - 8 * section.initialBlock) until (0x13e - 8 * section.initialBlock)
                        )
                    )
                    if (programKey.toInt() != Util.thermometerProgramKey) {
                        Log.w(::DeliveryPlan.javaClass.name, "Payload has wrong program key.")
                        return null
                    }
                }
                if (section.initialBlock == 0.toByte()) {
                    val header = section.data.sliceArray(0 until 0x18)
                    val storedChecksum =
                        Util.littleEndianDecode(
                            header.sliceArray(0 until 2)
                        )
                    val crc = CRC()
                    for (b in header.sliceArray(2 until header.size)) {
                        crc.update(b)
                    }
                    val computedChecksum = crc.getCRC()
                    if (storedChecksum.toShort() != computedChecksum) {
                        Log.w(::DeliveryPlan.javaClass.name, "Payload fails checksum.")
                        return null
                    }
                }
            }
            return plan
        }

        /**
         * Use the TITXTParser to convert the ASCII data into a binary representation.
         */
        private fun parse(text: String): DeliveryPlan? {
            try {
                val instructions = TITXTParser().process(text)
                var programmingKeyFound = false
                val sections = ArrayList<DeliverySection>()
                for ((startAddress, data) in instructions) {
                    if (startAddress % 8 != 0) {
                        Log.w(
                            ::DeliveryPlan.javaClass.name,
                            "Target addresses must be divisible by 8."
                        )
                        return null
                    }
                    val startBlock = (startAddress - libreBaseAddress) / 8
                    if (data.size % 8 != 0 || data.size + startAddress > 0x10000) {
                        Log.w(
                            ::DeliveryPlan.javaClass.name,
                            "Payload length must be divisible by 8 and not run over the end."
                        )
                        return null
                    }
                    val endBlock = (startAddress - libreBaseAddress + data.size) / 8
                    if (startBlock <= 39 && endBlock >= 39) {
                        programmingKeyFound = true
                    }
                    if (startBlock in 1..2 || endBlock in 0..1) {
                        Log.w(
                            ::DeliveryPlan.javaClass.name,
                            "Payload must contain all or none of the first area."
                        )
                        return null
                    }
                    sections.add(DeliverySection(startBlock.toByte(), data))
                }
                if (!programmingKeyFound) {
                    Log.w(::DeliveryPlan.javaClass.name, "Payload must contain programming key.")
                    return null
                }
                return DeliveryPlan(sections)
            } catch (e: TITXTParser.ParseException) {
                Log.e(::DeliveryPlan.javaClass.name, "Payload failed to parse.", e)
                return null
            }
        }

        /**
         * Create a delivery plan from textual data in TI-TXT format.
         */
        fun create(text: String): DeliveryPlan? {
            return parse(text)?.let { verify(it) }
        }
    }
}