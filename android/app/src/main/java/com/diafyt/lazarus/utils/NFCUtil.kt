package com.diafyt.lazarus.utils

import android.nfc.Tag

/**
 * Provide easy to use NFC commands.
 */
object NFCUtil {
    private const val blocklen = 8

    /**
     * Write a single block via the corresponding NFC command.
     */
    suspend fun writeBlock(tag: Tag, pos: Byte, data: ByteArray): ByteArray? {
        if (data.size != blocklen) {
            throw RuntimeException("Wrong size of payload (must be $blocklen bytes).")
        }
        val cmd = byteArrayOf(
            0x22, // flags: addressed (= UID field present) and high data rate mode
            0x21, // write single block
            0x00,  0x00, 0x00, 0x00, 0x00,  0x00, 0x00, 0x00, // placeholder for tag UID
            pos
        ) + data
        System.arraycopy(tag.id, 0, cmd, 2, 8)
        AsyncNFCTask(tag).asyncRun(cmd)?.let {
            return checkError(it)
        }
        return null
    }

    /**
     * Write multiple blocks via the corresponding NFC command.
     */
    suspend fun writeMultipleBlocks(tag: Tag, pos: Byte, data: ByteArray): ByteArray? {
        if (data.size < blocklen || data.size % blocklen != 0) {
            throw RuntimeException("Wrong size of payload (must be divisible by $blocklen bytes).")
        }
        val cmd = byteArrayOf(
            0x22, // flags: addressed (= UID field present) and high data rate mode
            0x24, // write multiple blocks
            0x00,  0x00, 0x00, 0x00, 0x00,  0x00, 0x00, 0x00, // placeholder for tag UID
            pos,
            (data.size / blocklen - 1).toByte()
        ) + data
        System.arraycopy(tag.id, 0, cmd, 2, 8)
        AsyncNFCTask(tag).asyncRun(cmd)?.let {
            return checkError(it)
        }
        return null
    }

    /**
     * Read a single block via the corresponding NFC command.
     *
     * This returns just the payload without status flags.
     */
    suspend fun readBlock(tag: Tag, pos: Byte): ByteArray? {
        val cmd = byteArrayOf(
             0x22, // flags: addressed (= UID field present) and high data rate mode
             0x20, // read single block
             0x00,  0x00, 0x00, 0x00, 0x00,  0x00, 0x00, 0x00, // placeholder for tag UID
             pos)
        System.arraycopy(tag.id, 0, cmd, 2, 8)
        AsyncNFCTask(tag).asyncRun(cmd)?.let {
            return checkError(it)
        }
        return null
    }

    /**
     * Read multiple blocks via the corresponding NFC command.
     *
     * This returns just the payload without status flags.
     */
    suspend fun readMultipleBlocks(tag: Tag, pos: Byte, count: Byte): ByteArray? {
        val cmd = byteArrayOf(
            0x22, // flags: addressed (= UID field present) and high data rate mode
            0x23, // read multiple blocks
            0x00,  0x00, 0x00, 0x00, 0x00,  0x00, 0x00, 0x00, // placeholder for tag UID
            pos,
            (count - 1).toByte()
        )
        System.arraycopy(tag.id, 0, cmd, 2, 8)
        AsyncNFCTask(tag).asyncRun(cmd)?.let {
            return checkError(it)
        }
        return null
    }

    /**
     * Check the status flags of a raw NFC response.
     *
     * In case of error return null otherwise return the response without the status flags.
     */
    fun checkError(msg: ByteArray): ByteArray? {
        if (msg.isNotEmpty() && msg[0] == 0.toByte()) {
            return msg.sliceArray(1 until  msg.size)
        }
        return null
    }
}