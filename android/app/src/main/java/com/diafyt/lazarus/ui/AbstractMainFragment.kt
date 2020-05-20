package com.diafyt.lazarus.ui

import android.nfc.Tag
import androidx.fragment.app.Fragment

/**
 * Base class for all fragments displayed in the MainActivity.
 *
 * This is basically an interface to provide some properties and methods.
 */
abstract class AbstractMainFragment : Fragment() {
    /**
     * resource id to be used as title in the ViewPager
     */
    abstract val tabTitleKey: Int

    /**
     * NFC event handler.
     *
     * On an NFC event an intent will be delivered to the MainActivity
     * which will call method on the current fragment to delegate the handling.
     */
    abstract fun handleNfc(tag: Tag)
}