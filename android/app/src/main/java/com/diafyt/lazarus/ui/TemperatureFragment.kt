package com.diafyt.lazarus.ui

import android.nfc.Tag
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.viewpager2.widget.ViewPager2
import com.diafyt.lazarus.R
import com.diafyt.lazarus.utils.AsyncNFCTask
import com.diafyt.lazarus.utils.NFCUtil
import com.diafyt.lazarus.utils.Util
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.round


/**
 * Read the temperature from a reprogrammed sensor.
 */
class TemperatureFragment : AbstractMainFragment() {
    override val tabTitleKey = R.string.tab_title_temperature
    private val keyUseFahrenheit = "useFahrenheit"
    private val keyLastMeasurement = "lastMeasurement"

    private lateinit var resultText: TextView
    private lateinit var unitSwitch: SwitchCompat
    private lateinit var progressBar: ProgressBar

    // transient state
    private var job: Job? = null

    // conserved state
    private var lastMeasurement: Double? = null // in degrees Celsius

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        if (savedInstanceState?.containsKey(keyLastMeasurement) == true) {
            lastMeasurement = savedInstanceState.getDouble(keyLastMeasurement)
        }

        val ret = inflater.inflate(R.layout.fragment_temperature, container, false)

        resultText = ret.findViewById(R.id.text_thermometer_result)
        unitSwitch = ret.findViewById(R.id.unit_switch)
        progressBar = ret.findViewById(R.id.sensor_progress)

        unitSwitch.setOnClickListener { unitSwitchClick() }

        updateUI()

        return ret
    }

    /**
     * Handle user input.
     */
    private fun unitSwitchClick() {
        PreferenceManager.getDefaultSharedPreferences(activity?.applicationContext).edit().apply {
            putBoolean(keyUseFahrenheit, unitSwitch.isChecked)
            apply()
        }
        updateUI()
    }

    /**
     * Refresh the UI to be consistent with the internal state.
     */
    private fun updateUI() {
        val useFahrenheit =
            PreferenceManager.getDefaultSharedPreferences(activity?.applicationContext).getBoolean(
                keyUseFahrenheit, false)
        unitSwitch.isChecked = useFahrenheit
        lastMeasurement.let {
            val numericValue = it?.let {
                if (useFahrenheit) {
                    Util.celsiusToFahrenheit(it)
                } else {
                    it
                }
            }
            val roundedValue = numericValue?.let { round(it * 10) / 10.0 }
            val stringValue = roundedValue?.toString() ?: "–"
            val unit = if (useFahrenheit) "°F" else "°C"
            resultText.text = getString(R.string.screen_thermometer_result, stringValue, unit)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        lastMeasurement?.let { outState.putDouble(keyLastMeasurement, it) }
    }

    override fun handleNfc(tag: Tag) {
        if (job != null) {
            Util.showInfoSnack(
                view,
                R.string.snackbar_nfc_overlap
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
                    when {
                        programKey <  2.0.pow(15.0) -> {
                            AlertDialog.Builder(it).apply {
                                setNegativeButton(R.string.button_title_no) { _, _ -> }
                                setPositiveButton(R.string.button_title_go_to_tutorial) { _, _ ->
                                    val pager = it.findViewById<ViewPager2>(R.id.pager)
                                    pager.setCurrentItem(0, false)
                                }
                                setTitle(getString(R.string.dialog_title_programmable_sensor))
                                setMessage(R.string.message_programmable_sensor)
                                show()
                            }
                        }
                        programKey != Util.thermometerProgramKey -> {
                            Util.showInfoDialog(
                                it,
                                R.string.dialog_title_no_thermometer,
                                R.string.message_no_thermometer
                            )
                        }
                        else -> {
                            readTag(tag)
                        }
                    }
                }
            } finally {
                progressBar.visibility = View.INVISIBLE
                job = null
            }
        }
    }

    /**
     * Convert the raw value received from the reprogrammed sensor into
     * an actual temperature value in degree Celsius.
     */
    private fun temperatureCalibration(raw: Int): Double {
        val r = raw + 411.737
        val kelvin = steinharthart(
            a=0.000679241, b=0.000324031, c=-0.000000173770, d=-0.0000000000677986, r=r)
        return kelvin - 273.15
    }

    /**
     * Implement the Steinhart-Hart equation calculating the dependency between
     * temperature and resistance of a thermistor.
     */
    private fun steinharthart(a: Double, b: Double, c: Double, d: Double, r: Double): Double {
        return if (r > 0) {
            1.0 / (a + b*ln(r) + c* ln(r).pow(3.0) + d* ln(r).pow(2.0))
        } else {
            0.0
        }
    }

    /**
     * Perform the actual temperature measurement.
     */
    private suspend fun readTag(tag: Tag) {
        Log.i(javaClass.name, "Retrieving temperature reading.")
        val cmd = byteArrayOf(
            0x02, // flags: high data rate mode
            0xB3.toByte(), // unlock blocks
            0x07 // vendor code
        )
        // First a request to get the measurement hardware to run
        AsyncNFCTask(tag).asyncRun(cmd)?.let {
            Log.d(javaClass.name, "Throwaway answer was " + Util.bytesToHex(it))
            NFCUtil.checkError(it)
        }
        // Wait a bit so the measurement hardware has time to do its thing
        delay(42)
        // Finally retrieve the measurement
        val result = AsyncNFCTask(tag).asyncRun(cmd)?.let {
            Log.i(javaClass.name, "Answer was " + Util.bytesToHex(it))
            NFCUtil.checkError(it)
        }
        if (result == null) {
            Log.w(javaClass.name, "Measurement failed.")
            return
        }
        Log.i(javaClass.name, "Measurement done.")
        val raw = Util.littleEndianDecode(result).toInt()
        lastMeasurement = temperatureCalibration(raw)
        updateUI()
        Log.i(javaClass.name, "Retrieved temperature reading.")
    }
}