package com.diafyt.lazarus.ui

import android.util.Log
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import kotlin.reflect.KClass

/**
 * Customized variant of FragmentStateAdapter instantiating fragments
 * according to the array of types passed.
 */
class MainFragmentStateAdapter(
    private val mainActivity: MainActivity,
    /**
     * classes of the fragments to be instantiated
     */
    private val fragments: Array<KClass<out AbstractMainFragment>>
) : FragmentStateAdapter(mainActivity) {

    override fun getItemCount(): Int = fragments.size

    override fun createFragment(position: Int): Fragment {
        val ret = fragments[position].constructors.first().call()
        mainActivity.fragmentStore[position] = ret
        Log.d(javaClass.name, "Create fragment $ret for position $position")
        return ret
    }
}