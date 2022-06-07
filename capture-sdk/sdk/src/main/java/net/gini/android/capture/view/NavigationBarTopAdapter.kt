package net.gini.android.capture.view

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.R
import net.gini.android.capture.databinding.GcNavigationBarTopBinding

/**
 * Created by Alpár Szotyori on 13.05.22.
 *
 * Copyright (c) 2022 Gini GmbH.
 */

interface NavigationBarTopAdapter : InjectedViewAdapter {
    fun setOnNavButtonClickListener(listener: View.OnClickListener?)
    fun setTitle(title: String)
}

/**
 * Created by Alpár Szotyori on 13.05.22.
 *
 * Copyright (c) 2022 Gini GmbH.
 */
internal class DefaultNavigationBarTopAdapter : NavigationBarTopAdapter {

    var viewBinding: GcNavigationBarTopBinding? = null

    override fun setOnNavButtonClickListener(listener: View.OnClickListener?) {
        if (GiniCapture.hasInstance()
            && GiniCapture.getInstance().isBottomNavigationBarEnabled
        ) {
            viewBinding?.gcNavigationBar?.setOnMenuItemClickListener {
                listener?.onClick(viewBinding?.root)
                true
            }
        } else {
            viewBinding?.gcNavigationBar?.setNavigationOnClickListener(listener)
        }
    }

    override fun setTitle(title: String) {
        viewBinding?.gcNavigationBar?.title = title
    }

    override fun getView(container: ViewGroup): View {
        viewBinding?.let { return it.root }

        val binding = GcNavigationBarTopBinding
            .inflate(LayoutInflater.from(container.context), container, false)
        viewBinding = binding

        return binding.root
    }

    override fun onDestroy() {
        viewBinding = null
    }

}