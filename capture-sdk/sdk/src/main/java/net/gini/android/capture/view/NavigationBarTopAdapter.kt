package net.gini.android.capture.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.annotation.MenuRes
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.R
import net.gini.android.capture.databinding.GcNavigationBarTopBinding
import net.gini.android.capture.view.NavButtonType.BACK
import net.gini.android.capture.view.NavButtonType.CLOSE
import net.gini.android.capture.view.NavButtonType.NONE

/**
 * Created by Alpár Szotyori on 13.05.22.
 *
 * Copyright (c) 2022 Gini GmbH.
 */

/**
 * Adapter for injecting a custom top navigation bar.
 */
interface NavigationBarTopAdapter : InjectedViewAdapter {
    /**
     * Set the click listener for the navigation bar's navigation button.
     *
     * @param listener the click listener for the button
     */
    fun setOnNavButtonClickListener(listener: View.OnClickListener?)

    /**
     * Set the navigation bar title.
     *
     * @param title navigation bar title
     */
    fun setTitle(title: String)

    /**
     * Called when the navigation button type has to change. You should update the look of the navigation button
     * based on its type.
     *
     * @param navButtonType the type of the navigation button
     */
    fun setNavButtonType(navButtonType: NavButtonType)


    fun setMenuResource(@MenuRes menu: Int)


    fun setOnMenuItemClickListener(menuItem: Toolbar.OnMenuItemClickListener)
}

/**
 * Navigation button types.
 */
enum class NavButtonType {
    /**
     * Navigation button is used as a back button.
     */
    BACK,

    /**
     * Navigation button is used as a close button.
     */
    CLOSE,

    /**
     * Used when no button is shown on the top bar.
     */
    NONE
}

/**
 * Created by Alpár Szotyori on 13.05.22.
 *
 * Copyright (c) 2022 Gini GmbH.
 */

/**
 * Internal use only
 *
 * @suppress
 */
class DefaultNavigationBarTopAdapter : NavigationBarTopAdapter {

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
        when (navButtonType) {
            NONE -> {
                //Used when bottom bar navigation is enabled
            }
            BACK -> {
                viewBinding?.root?.context?.let { context ->
                    viewBinding?.gcNavigationBar?.navigationIcon =
                        ContextCompat.getDrawable(context, R.drawable.gc_action_bar_back)
                    viewBinding?.gcNavigationBar?.navigationContentDescription =
                        context.getString(R.string.gc_back_button_description)

                }
            }
            CLOSE -> {
                if (GiniCapture.hasInstance()
                    && GiniCapture.getInstance().isBottomNavigationBarEnabled
                ) {
                    viewBinding?.gcNavigationBar?.menu?.clear()
                    viewBinding?.gcNavigationBar?.inflateMenu(R.menu.gc_navigation_bar_top_close)
                } else {
                    viewBinding?.root?.context?.let { context ->
                        viewBinding?.gcNavigationBar?.navigationIcon =
                            ContextCompat.getDrawable(context, R.drawable.gc_close)
                        viewBinding?.gcNavigationBar?.navigationContentDescription =
                            context.getString(R.string.gc_close)
                    }
                }
            }
        }
    }

    override fun setMenuResource(menu: Int) {
        viewBinding?.gcNavigationBar?.menu?.clear()
        viewBinding?.gcNavigationBar?.inflateMenu(menu)
    }

    override fun setOnMenuItemClickListener(menuItem: Toolbar.OnMenuItemClickListener) {
        viewBinding?.gcNavigationBar?.setOnMenuItemClickListener(menuItem)
    }

    override fun onCreateView(container: ViewGroup): View {
        val binding = GcNavigationBarTopBinding
            .inflate(LayoutInflater.from(container.context), container, false)
        viewBinding = binding

        return binding.root
    }

    override fun onDestroy() {
        viewBinding = null
    }

    fun setBackgroundColor(@ColorInt colorInt: Int) {
        viewBinding?.gcNavigationBar?.setBackgroundColor(colorInt)
    }

}