package net.gini.android.capture.view

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import net.gini.android.capture.databinding.GcNavigationBarBottomBinding

/**
 * Created by Alpár Szotyori on 13.05.22.
 *
 * Copyright (c) 2022 Gini GmbH.
 */

interface NavigationBarBottomAdapter : InjectedViewAdapter {
    fun setOnBackButtonClickListener(listener: View.OnClickListener?)
    fun setBackButtonIcon(icon: Drawable)
}

/**
 * Created by Alpár Szotyori on 13.05.22.
 *
 * Copyright (c) 2022 Gini GmbH.
 */
internal class DefaultNavigationBarBottomAdapter : NavigationBarBottomAdapter {

    var viewBinding: GcNavigationBarBottomBinding? = null

    override fun setOnBackButtonClickListener(listener: View.OnClickListener?) {
        viewBinding?.gcNavigationBar?.setNavigationOnClickListener(listener)
    }

    override fun setBackButtonIcon(icon: Drawable) {
        viewBinding?.gcNavigationBar?.navigationIcon = icon
    }

    override fun getView(container: ViewGroup): View {
        viewBinding?.let { return it.root }

        val binding = GcNavigationBarBottomBinding
            .inflate(LayoutInflater.from(container.context), container, false)
        viewBinding = binding

        return binding.root
    }

    override fun onDestroy() {
        viewBinding = null
    }

}