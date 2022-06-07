package net.gini.android.capture.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.findViewTreeLifecycleOwner
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Created by Alpár Szotyori on 13.05.22.
 *
 * Copyright (c) 2022 Gini GmbH.
 */

class InjectedViewContainer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    var injectedViewAdapter: InjectedViewAdapter? = null
        set(value) {
            if (value == null) {
                field = value
                removeInjectedView()
                return
            }
            if (field != value) {
                field = value
                injectView()
            }
        }

    private var injectedView: View? = null

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        LOG?.debug("Get lifecycle")
        findViewTreeLifecycleOwner()?.lifecycle?.let { lifecycle ->
            LOG?.debug("Add lifecycle observer")
            lifecycle.addObserver(object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    LOG?.debug("Destroy injected view provider")
                    injectedViewAdapter?.onDestroy()
                }
            })
        }
    }

    private fun removeInjectedView() {
        LOG?.debug("Remove injected view")
        if (injectedView != null) {
            removeView(injectedView)
            injectedView = null
            LOG?.debug("Injected view removed")
        }
    }

    private fun injectView() {
        LOG?.debug("Inject view")
        removeInjectedView()
        injectedViewAdapter?.getView(container = this)?.let { view ->
            view.layoutParams =
                LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    .apply {
                        topToTop = LayoutParams.PARENT_ID
                        bottomToBottom = LayoutParams.PARENT_ID
                        startToStart = LayoutParams.PARENT_ID
                        endToEnd = LayoutParams.PARENT_ID
                    }
            addView(view)
            injectedView = view
            LOG?.debug("View injected")
        }
    }

    companion object {
        private const val DEBUG = true
        private val LOG: Logger? = if (DEBUG) LoggerFactory.getLogger(InjectedViewContainer::class.java) else null
    }

}