package com.diafyt.lazarus.ui

import android.nfc.Tag
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.diafyt.lazarus.R
import com.diafyt.lazarus.utils.Util

/**
 * Show usage information.
 */
class TutorialFragment : AbstractMainFragment() {
    override val tabTitleKey = R.string.tab_title_tutorial

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_tutorial, container, false)
    }

    override fun handleNfc(tag: Tag) {
        Util.showInfoSnack(
            view,
            R.string.snackbar_tutorial_nfc_intent
        )
    }

}