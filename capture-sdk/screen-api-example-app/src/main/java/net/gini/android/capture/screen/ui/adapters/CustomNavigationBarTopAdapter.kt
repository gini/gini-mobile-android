package net.gini.android.capture.screen.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.R
import net.gini.android.capture.databinding.CustomNavigationBarTopBinding
import net.gini.android.capture.databinding.GcNavigationBarTopBinding
import net.gini.android.capture.view.NavButtonType
import net.gini.android.capture.view.NavigationBarTopAdapter

class CustomNavigationBarTopAdapter : NavigationBarTopAdapter {

    var viewBinding: CustomNavigationBarTopBinding? = null

    override fun setOnNavButtonClickListener(listener: View.OnClickListener?) {
        if (GiniCapture.hasInstance()
            && GiniCapture.getInstance().isBottomNavigationBarEnabled
        ) {
            viewBinding?.materialToolbarNavigationBar?.setOnMenuItemClickListener {
                listener?.onClick(viewBinding?.root)
                true
            }
        } else {
            viewBinding?.materialToolbarNavigationBar?.setNavigationOnClickListener(listener)
        }
    }

    override fun setTitle(title: String) {
        viewBinding?.materialToolbarNavigationBar?.title = title
    }

    override fun setNavButtonType(navButtonType: NavButtonType) {
        when (navButtonType) {
            NavButtonType.NONE -> {
                //Used when bottom bar navigation is enabled
            }
            NavButtonType.BACK -> {
                viewBinding?.root?.context?.let { context ->
                    viewBinding?.materialToolbarNavigationBar?.navigationIcon =
                        ContextCompat.getDrawable(context, R.drawable.gc_action_bar_back)
                    viewBinding?.materialToolbarNavigationBar?.navigationContentDescription =
                        context.getString(R.string.gc_back_button_description)

                }
            }
            NavButtonType.CLOSE -> {
                if (GiniCapture.hasInstance()
                    && GiniCapture.getInstance().isBottomNavigationBarEnabled
                ) {
                    viewBinding?.materialToolbarNavigationBar?.menu?.clear()
                    viewBinding?.materialToolbarNavigationBar?.inflateMenu(R.menu.gc_navigation_bar_top_close)
                } else {
                    viewBinding?.root?.context?.let { context ->
                        viewBinding?.materialToolbarNavigationBar?.navigationIcon =
                            ContextCompat.getDrawable(context, R.drawable.gc_close)
                        viewBinding?.materialToolbarNavigationBar?.navigationContentDescription =
                            context.getString(R.string.gc_close)
                    }
                }
            }
        }
    }

    override fun setMenuResource(menu: Int) {
        viewBinding?.materialToolbarNavigationBar?.menu?.clear()
        viewBinding?.materialToolbarNavigationBar?.inflateMenu(menu)
    }

    override fun setOnMenuItemClickListener(menuItem: Toolbar.OnMenuItemClickListener) {
        viewBinding?.materialToolbarNavigationBar?.setOnMenuItemClickListener(menuItem)
    }

    override fun onCreateView(container: ViewGroup): View {
        val binding = CustomNavigationBarTopBinding
            .inflate(LayoutInflater.from(container.context), container, false)
        viewBinding = binding

        return binding.root
    }

    override fun onDestroy() {
        viewBinding = null
    }

    fun setBackgroundColor(@ColorInt colorInt: Int) {
        viewBinding?.materialToolbarNavigationBar?.setBackgroundColor(colorInt)
    }

}