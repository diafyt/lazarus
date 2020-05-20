package com.diafyt.lazarus.utils

import android.content.Context
import android.nfc.Tag
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.diafyt.lazarus.R
import com.google.android.material.snackbar.Snackbar

/**
 * Various utility functions.
 */
object Util {
    /**
     * Runtime of the Libre Freestyle sensor in minutes.
     */
    const val libreRuntime = 14 * 24 * 60

    /**
     * Signature used to recognize sensors reprogrammed by this app.
     */
    const val thermometerProgramKey = 0x8001

    /**
     * Convert a series of hex characters into binary data.
     *
     * The number of characters must be even.
     */
    fun hexToBytes(s: CharSequence): ByteArray? {
        val stripped = s.replace(Regex(" "), "")
        if (stripped.length % 2 == 1) {
            return null
        }
        val ret = ArrayList<Byte>()
        var idx = 0
        while (idx < stripped.length) {
            ret.add(stripped.substring(idx, idx + 2).toInt(16).toByte())
            idx += 2
        }
        return ret.toByteArray()
    }

    /**
     * Convert binary data into hex characters.
     */
    fun bytesToHex(bs: ByteArray, prefix: String = "0x", joiner: String = " "): String {
        val builder = StringBuilder()
        var effectiveJoiner = ""
        for (b in bs) {
            builder.append(effectiveJoiner + prefix + unsignedByteToInt(
                b
            ).toString(16))
            effectiveJoiner = joiner
        }
        return builder.toString()
    }

    /**
     * Convert degree Celsius to degree Fahrenheit.
     */
    fun celsiusToFahrenheit(c: Double): Double {
        return c * 1.8 + 32
    }

    /**
     * Wrapper around AlertDialog.Builder to show a dialog with a single button without effect.
     */
    fun showInfoDialog(context: Context, titleId: Int, messageId: Int) {
        AlertDialog.Builder(context).apply {
            setPositiveButton(R.string.button_title_ok) { _, _ -> }
            setMessage(messageId)
            setTitle(titleId)
            show()
        }
    }

    /**
     * Convert a byte to an integer treating the byte as unsigned.
     *
     * As we work on the JVM which does not provide unsigned data types
     * this is a tiny bit involved.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun unsignedByteToInt(b: Byte): Int {
        return b.toInt() and 0xFF
    }

    /**
     * Decode a series of bytes from little endian format into a single number.
     *
     * Example: [0x12, 0x34] becomes 0x3412
     */
    fun littleEndianDecode(bs: ByteArray): Long {
        var ret = 0L
        for (i in (bs.size - 1) downTo 0) {
            ret = (ret shl 8) + unsignedByteToInt(
                bs[i]
            )
        }
        return ret
    }

    /**
     * Wrapper around Snackbar.make() for easier use.
     */
    fun showInfoSnack(view: View?, msgId: Int) {
        view?.findViewById<View>(R.id.layout_coordinator_fragment)?.let {
            Snackbar.make(it, msgId, Snackbar.LENGTH_LONG).show()
        }
    }

    /**
     * Retrieve the program signature.
     *
     * This also checks whether the tag is a Libre 1. The program key uses the location of the
     * sensor time, meaning that this also enables detection of non-expired sensors.
     *
     * In case of a tag different from a Libre 1 null is returned.
     */
    suspend fun retrieveProgramKey(tag: Tag): Int? {
        if (tag.id[6] != 0x07.toByte() || tag.id[7] != 0xE0.toByte()) {
            // wrong manufacturer
            return null
        }
        val header = NFCUtil.readMultipleBlocks(tag, 0, 3) ?: return null
        val storedChecksum = littleEndianDecode(
            header.sliceArray(0 until 2)
        ).toShort()
        val crc = CRC()
        for (b in header.sliceArray(2 until header.size)) {
            crc.update(b)
        }
        val computedChecksum = crc.getCRC()
        if (storedChecksum != computedChecksum) {
            return null
        }
        val block39 = NFCUtil.readBlock(tag, 39) ?: return null
        if (block39.size != 8) {
            // block size different from Libre
            return null
        }
        return littleEndianDecode(
            block39.sliceArray(
                4 until 6
            )
        ).toInt()
    }
}