package com.diafyt.lazarus.utils

import android.util.Log
import com.diafyt.lazarus.BuildConfig
import java.io.*
import java.util.*

/**
 * Handle exception logging.
 *
 * This by default simply logs the exception to the standard Android logging
 * facility. In debug mode it additionally writes each exception out into a
 * file.
 *
 * This is a singleton and needs to be initialized with the output directory
 * path which comes from an Android context (this context may be unavailable
 * at some call sites).
 */
object ExceptionArchivist {
    private var persistenceDir: File? = null

    /**
     * Set up the context for the archivists work.
     *
     * @param persistenceDir path where files are to be store
     */
    fun initialize(persistenceDir: File?) {
        ExceptionArchivist.persistenceDir = persistenceDir
    }

    /**
     * Persist the passed message to a file.
     *
     * @param message message
     */
    private fun logToFile(message: String) {
        if (persistenceDir == null) {
            Log.w(javaClass.name, "No persistence directory supplied.")
            return
        }
        val cdate = Date()
        val logDir = File(persistenceDir, "log")
        var success = true
        if (!logDir.exists()) {
            success = logDir.mkdirs()
        }
        if (success) {
            val logFile = File(logDir, "exception--" + cdate.time + ".txt")
            try {
                if (logFile.createNewFile()) {
                    val writer = BufferedWriter(FileWriter(logFile))
                    writer.write(message)
                    writer.close()
                } else {
                    Log.w(javaClass.name, "Failed to create log file.")
                }
            } catch (e: IOException) {
                Log.e(javaClass.name, "Archiving exception failed.", e)
            }
        } else {
            Log.w(javaClass.name, "Failed to create log directory.")
        }
    }

    /**
     * Log an exception.
     *
     * @param e exception to log
     * @param message explanatory message
     * @param tag source identifier
     */
    fun log(
        e: Exception,
        message: String = "Exception caught.",
        tag: String = javaClass.name
    ) {
        Log.e(tag, message, e)
        if (BuildConfig.DEBUG) {
            val sw = StringWriter()
            val pw = PrintWriter(sw)
            pw.println(tag)
            pw.println(message)
            e.printStackTrace(pw)
            logToFile(sw.toString())
        }
    }
}
