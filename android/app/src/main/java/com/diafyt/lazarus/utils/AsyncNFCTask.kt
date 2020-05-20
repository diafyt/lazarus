package com.diafyt.lazarus.utils

import android.nfc.Tag
import android.nfc.tech.NfcV
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Perform NFC communication in an asynchronous manner.
 * This does the actual IO.
 */
class AsyncNFCTask(val tag: Tag) {
    private val timeout = 1000

    /**
     * Perform a single transmission.
     *
     * This returns the answer with status flags.
     */
    suspend fun asyncRun(cmd: ByteArray): ByteArray? {
        val ret = asyncRun(listOf(cmd))
        if (ret.isNotEmpty()) {
            return ret[0]
        }
        return null
    }

    /**
     * Perform a bunch of transmissions.
     *
     * This returns the answers with status flags.
     */
    suspend fun asyncRun(cmds: List<ByteArray>): List<ByteArray?> {
        var lastSuccess = System.currentTimeMillis()
        val nfcvTag = NfcV.get(tag)
        val ret = ArrayList<ByteArray?>()
        withContext(Dispatchers.IO) {
            try {
                nfcvTag.connect()
                for (cmd in cmds) {
                    if (!isActive) {
                        break
                    }
                    var answer: ByteArray? = null
                    while (isActive) {
                        try {
                            answer = nfcvTag.transceive(cmd)
                            lastSuccess = System.currentTimeMillis()
                            break
                        } catch (e: IOException) {
                            if (System.currentTimeMillis() > lastSuccess + timeout) {
                                break
                            }
                        }
                    }
                    ret.add(answer)
                    if (answer == null) {
                        break
                    }
                }
            } catch (e: Exception) {
                ExceptionArchivist.log(e)
            } finally {
                try {
                    nfcvTag.close()
                } catch (e: Exception) {
                    ExceptionArchivist.log(e)
                }
            }
        }
        return ret
    }
}