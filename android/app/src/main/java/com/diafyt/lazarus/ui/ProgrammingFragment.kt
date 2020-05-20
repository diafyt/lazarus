package com.diafyt.lazarus.ui

import android.nfc.Tag
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.diafyt.lazarus.*
import com.diafyt.lazarus.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.floor
import kotlin.math.pow

/**
 * Flashes the image onto the sensor.
 */
class ProgrammingFragment : AbstractMainFragment() {
    override val tabTitleKey = R.string.tab_title_programming

    private lateinit var progressBar: ProgressBar

    // transient state
    private var job: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val ret = inflater.inflate(R.layout.fragment_programming, container, false)
        progressBar = ret.findViewById(R.id.sensor_progress)
        return ret
    }

    override fun handleNfc(tag: Tag) {
        if (job != null) {
            Util.showInfoSnack(
                view,
                R.string.snackbar_programming_overlap
            )
            return
        }
        job = viewLifecycleOwner.lifecycleScope.launch {
            progressBar.visibility = View.VISIBLE
            try {
                activity?.let {
                    val programKey =
                        Util.retrieveProgramKey(tag)
                    if (programKey == null) {
                        Util.showInfoDialog(
                            it,
                            R.string.dialog_title_no_libre1,
                            R.string.message_no_libre1
                        )
                        return@launch
                    }
                    if (programKey <= Util.libreRuntime) {
                        Util.showInfoDialog(
                            it,
                            R.string.dialog_title_libre1_running,
                            R.string.message_libre1_running
                        )
                        return@launch
                    }
                    if (programKey >= floor(2.0.pow(15.0))) {
                        val msg = if (programKey == Util.thermometerProgramKey) {
                            R.string.message_confirm_self_overwrite
                        } else {
                            R.string.message_confirm_foreign_overwrite
                        }
                        AlertDialog.Builder(it).apply {
                            setNegativeButton(R.string.button_title_no) { _, _ -> }
                            setPositiveButton(R.string.button_title_yes) { _, _ ->
                                job = viewLifecycleOwner.lifecycleScope.launch {
                                    progressBar.visibility = View.VISIBLE
                                    try {
                                        programTag(tag, false)
                                    } finally {
                                        progressBar.visibility = View.INVISIBLE
                                        job = null
                                    }
                                }
                            }
                            setTitle(getString(R.string.dialog_title_confirm_overwrite))
                            setMessage(msg)
                            show()
                        }
                    } else {
                        programTag(tag, true)
                    }
                }
            } finally {
                progressBar.visibility = View.INVISIBLE
                job = null
            }
        }
    }

    /**
     * Actually flash the image.
     */
    private suspend fun programTag(tag: Tag, initialize: Boolean) {
        Log.i(javaClass.name, "Begin tag programming.")
        var success = false
        val initialized = if (initialize) {
            initializeTag(tag) != null
        } else {
            true
        }

        if (initialized) {
            withContext(Dispatchers.IO) {
                val text = activity?.assets?.open("thermometer-payload.txt")
                    ?.bufferedReader()?.use { it.readText() }
                val plan = text?.let { DeliveryPlan.create(it) }
                success = plan?.deliver(tag) ?: false
            }
        }

        if (success) {
            Util.showInfoSnack(
                view,
                R.string.snackbar_programming_successful
            )
        } else {
            Util.showInfoSnack(
                view,
                R.string.snackbar_programming_error
            )
        }
        Log.i(javaClass.name, "Tag programming complete.")
    }

    /**
     * Prepare a pristine Libre to be flashed.
     */
    private suspend fun initializeTag(tag: Tag) : ByteArray? {
        Log.i(javaClass.name, "Initialize tag.")
        // Removed due to legal reasons
        Log.i(javaClass.name, "Tag initialization failed.")
        return null
    }
}

