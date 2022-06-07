package net.gini.android.capture.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.R
import net.gini.android.capture.databinding.GcNavigationBarTopBinding
import net.gini.android.capture.view.NavButtonType.BACK
import net.gini.android.capture.view.NavButtonType.CLOSE

/**
 * Created by Alpár Szotyori on 13.05.22.
 *
 * Copyright (c) 2022 Gini GmbH.
 */

interface NavigationBarTopAdapter : InjectedViewAdapter {
    fun setOnNavButtonClickListener(listener: View.OnClickListener?)
    fun setTitle(title: String)
    fun setNavButtonType(navButtonType: NavButtonType)
}

enum class NavButtonType {
    BACK,
    CLOSE
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

    override fun setNavButtonType(navButtonType: NavButtonType) {
        if (GiniCapture.hasInstance()
            && GiniCapture.getInstance().isBottomNavigationBarEnabled
        ) {
            when (navButtonType) {
                BACK -> {
                    // Not used when bottom navigation bar is enabled
                }
                CLOSE -> {
                    viewBinding?.gcNavigationBar?.inflateMenu(R.menu.gc_navigation_bar_top_close)
                }
            }
        } else {
            when (navButtonType) {
                BACK -> {
                    viewBinding?.root?.context?.let { context ->
                        viewBinding?.gcNavigationBar?.navigationIcon =
                            ContextCompat.getDrawable(context, R.drawable.gc_action_bar_back)
                    }
                }
                CLOSE -> {
                    viewBinding?.root?.context?.let { context ->
                        viewBinding?.gcNavigationBar?.navigationIcon =
                            ContextCompat.getDrawable(context, R.drawable.gc_close)
                    }
                }
            }
        }
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