package com.diafyt.lazarus.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.diafyt.lazarus.R

/**
 * Show an informational screen about Lazarus.
 */
class AboutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)
        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
