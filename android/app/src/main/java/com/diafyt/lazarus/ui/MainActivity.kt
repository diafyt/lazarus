package com.diafyt.lazarus.ui

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.NfcV
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.diafyt.lazarus.*
import com.diafyt.lazarus.utils.ExceptionArchivist
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlin.math.min

/**
 * Central activity providing nearly all screens.
 *
 * This also handles the NFC setup.
 */
class MainActivity : AppCompatActivity() {
    private val keyFragmentStore = "fragmentStore"

    private lateinit var mainFragmentStateAdapter: MainFragmentStateAdapter
    private lateinit var viewPager: ViewPager2

    // conserved state
    /**
     * The actual fragments used in the ViewPager, will be updated by the MainFragmentStateAdapter.
     */
    lateinit var fragmentStore: HashMap<Int, AbstractMainFragment>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fragmentStore = HashMap()
        @Suppress("UNCHECKED_CAST") val serializedStore = (
                savedInstanceState?.getSerializable(keyFragmentStore)
                        as? HashMap<Int, String>) ?: HashMap()
        for (i in serializedStore.keys) {
            serializedStore[i]?.let { tag ->
                supportFragmentManager.findFragmentByTag(tag)?.let {
                    fragmentStore[i] = it as AbstractMainFragment
                }
            }
        }
        Log.d(
            javaClass.name,
            "Deserialized fragmentStore: found " + fragmentStore.size
                    + " of " + serializedStore.size)
        setContentView(R.layout.activity_main)

        ExceptionArchivist.initialize(getExternalFilesDir(null))
        initFragments()

        val adapter = NfcAdapter.getDefaultAdapter(this)
        if (adapter == null) {
            AlertDialog.Builder(this).apply {
                setNegativeButton(R.string.button_title_close) { _, _ -> finish() }
                setPositiveButton(R.string.button_title_use_anyway) { _, _ -> }
                setTitle(getString(R.string.dialog_title_no_nfc))
                setMessage(R.string.message_error_nfc_missing)
                show()
            }
        } else {
            if (!adapter.isEnabled) {
                val s = getString(R.string.snackbar_error_nfc_disabled)
                Snackbar.make(viewPager, s, Snackbar.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Set up the ViewPager.
     */
    private fun initFragments() {
        val fragments = arrayOf(
            TutorialFragment::class, ProgrammingFragment::class, TemperatureFragment::class)

        viewPager = findViewById(R.id.pager)
        val pos = if(viewPager.adapter == null) {
            // on first iteration start on the temperature screen
            fragments.indexOf(TemperatureFragment::class)
        } else {
            // otherwise keep the current screen
            min(viewPager.currentItem, fragments.size - 1)
        }
        mainFragmentStateAdapter =
            MainFragmentStateAdapter(this, fragments)
        viewPager.adapter = mainFragmentStateAdapter
        val tabLayout: TabLayout = findViewById(R.id.tab_layout)
        tabLayout.tabMode = TabLayout.MODE_SCROLLABLE
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            val fragment =
                fragmentStore[position] ?: fragments[position].constructors.first().call()
            tab.text = getString(fragment.tabTitleKey)
        }.attach()
        viewPager.setCurrentItem(pos, false)
    }

    /**
     * Query which fragment is currently shown by the ViewPager.
     */
    private fun currentFragment(): AbstractMainFragment? {
        return fragmentStore[viewPager.currentItem]
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onResume() {
        super.onResume()
        val adapter = NfcAdapter.getDefaultAdapter(this)
        if (adapter != null) {
            val intent = Intent(this, javaClass).apply {
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            val pendingIntent = PendingIntent.getActivity(
                this, 0, intent, 0
            )
            if (pendingIntent != null) {
                adapter.enableForegroundDispatch(
                    this, pendingIntent,
                    arrayOf(IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED)),
                    arrayOf(arrayOf(NfcV::class.java.name))
                )
                Log.i(javaClass.name, "NFC foreground dispatch enabled.")
            } else {
                Log.w(
                    javaClass.name,
                    "No pending intent. Foreground dispatch not initialized.")

            }
        } else {
            Log.w(javaClass.name, "No NFC adapter. Foreground dispatch not initialized.")
        }
    }

    override fun onPause() {
        NfcAdapter.getDefaultAdapter(this)?.disableForegroundDispatch(this)
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val serializableStore = HashMap<Int, String>()
        for (i in fragmentStore.keys) {
            fragmentStore[i]?.let {fragment ->
                fragment.tag?.let {
                    serializableStore[i] = it
                }
            }
        }
        Log.d(
            javaClass.name,
            "Serialized fragmentStore: found tags for " + serializableStore.size
                    + " of " + fragmentStore.size)
        outState.putSerializable(keyFragmentStore, serializableStore)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_open_diafyt_website -> {
                val browserIntent = Intent(
                    Intent.ACTION_VIEW, Uri.parse("https://www.diafyt.com/"))
                startActivity(browserIntent)
                true
            }
            R.id.action_open_lazarus_website -> {
                val intent = Intent(this, AboutActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        when (intent.action) {
            NfcAdapter.ACTION_TECH_DISCOVERED -> {
                val tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG) as? Tag
                if (tag != null) {
                    currentFragment()?.handleNfc(tag)
                } else {
                    Log.w(javaClass.name, "NFC tag missing.")
                }
            }
        }
    }
}

