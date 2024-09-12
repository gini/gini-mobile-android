package net.gini.android.merchant.sdk.util.extensions

import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager

internal fun FragmentManager.add(@IdRes containerId: Int, fragment: Fragment, addToBackStack: Boolean) {
    beginTransaction()
     .add(containerId, fragment, fragment::class.java.name)
     .apply { if (addToBackStack) addToBackStack(fragment::class.java.name) }
     .commit()
}