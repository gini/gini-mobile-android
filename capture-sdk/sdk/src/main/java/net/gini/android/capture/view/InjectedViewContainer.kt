package net.gini.android.capture.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.findViewTreeLifecycleOwner
import net.gini.android.capture.BuildConfig
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Created by Alpár Szotyori on 13.05.22.
 *
 * Copyright (c) 2022 Gini GmbH.
 */

/**
 * Internal use only.
 *
 * A container view for showing [InjectedViewAdapter]s. It uses the [InjectedViewAdapterHolder] in combination with the
 * [InjectedViewAdapterInstance] and [InjectedViewAdapterConfigurator] to display the view provided by the
 * [InjectedViewAdapter] and to configure the [InjectedViewAdapter].
 *
 * The [InjectedViewContainer] observes the lifecycle of the view tree lifecycle owner:
 *   * `onStart`: takes ownership of the [InjectedViewAdapter], (re)creates its view, and (re)configures it,
 *   * `onDestroy`: destroys the [InjectedViewAdapter] ONLY IF it owns it.
 *
 * IMPORTANT: In cases where an [InjectedViewAdapter] is used in [InjectedViewContainer]s on multiple screens
 * the strategy is that the [InjectedViewContainer] which entered the started state last will own the
 * [InjectedViewAdapter], will recreate its view, and will reconfigure it.
 */
class InjectedViewContainer<T: InjectedViewAdapter> @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    var injectedViewAdapterHolder: InjectedViewAdapterHolder<T>? = null
        set(value) {
            if (value == null) {
                if (field?.viewAdapterInstance?.viewContainer == this) {
                    LOG?.debug(
                        "{}: Remove container from view adapter instance {}",
                        this.hashCode(),
                        field?.viewAdapterInstance?.hashCode()
                    )
                    field?.viewAdapterInstance?.viewContainer = null
                }
                field = value
                removeInjectedView()
                return
            }
            if (field != value) {
                if (field?.viewAdapterInstance?.viewContainer == this) {
                    LOG?.debug(
                        "{}: Remove container from previous view adapter instance {}",
                        this,
                        field?.viewAdapterInstance
                    )
                    field?.viewAdapterInstance?.viewContainer = null
                }
                // Set container for the new adapter
                LOG?.debug(
                    "{}: Set container for view adapter instance {}",
                    this.hashCode(),
                    value.viewAdapterInstance.hashCode()
                )
                value.viewAdapterInstance.viewContainer = this
                field = value
            }
        }
    private var injectedView: View? = null

    /**
     * Use this method to modify the [InjectedViewAdapter]. It makes sure to only apply the modifications, if
     * the [InjectedViewAdapter] is still owned by this container.
     *
     * @param modify lambda containing the modifications to apply on the [InjectedViewAdapter]
     */
    fun modifyAdapterIfOwned(modify: (injectedViewAdapter: T) -> Unit) {
        injectedView?.let {  }
        if (injectedViewAdapterHolder?.viewAdapterInstance?.viewContainer == this) {
            injectedViewAdapterHolder?.viewAdapterInstance?.viewAdapter?.let { modify(it) }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        LOG?.debug("{}: Get lifecycle", this.hashCode())
        findViewTreeLifecycleOwner()?.lifecycle?.let { lifecycle ->
            LOG?.debug("{}: Add lifecycle observer", this.hashCode())
            lifecycle.addObserver(object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) {
                    injectedViewAdapterHolder?.let { viewAdapterHolder ->
                        viewAdapterHolder.viewAdapterInstance.viewContainer = this@InjectedViewContainer
                        LOG?.debug(
                            "{}: Show and configure injected view adapter for instance {}",
                            this@InjectedViewContainer.hashCode(),
                            viewAdapterHolder.viewAdapterInstance.hashCode()
                        )
                        injectView()
                        viewAdapterHolder.configureViewAdapter()
                    }
                }

                override fun onDestroy(owner: LifecycleOwner) {
                    injectedViewAdapterHolder?.let { viewAdapterHolder ->
                        if (viewAdapterHolder.viewAdapterInstance.viewContainer == this@InjectedViewContainer) {
                            LOG?.debug(
                                "{}: Destroy injected view adapter for instance {}",
                                this@InjectedViewContainer.hashCode(),
                                viewAdapterHolder.viewAdapterInstance.hashCode()
                            )
                            viewAdapterHolder.viewAdapterInstance.viewAdapter.onDestroy()
                        } else {
                            LOG?.debug(
                                "{}: Injected view adapter for instance {} is in another container and was not destroyed",
                                this@InjectedViewContainer.hashCode(),
                                viewAdapterHolder.viewAdapterInstance.hashCode()
                            )
                        }
                        injectedViewAdapterHolder = null
                    }
                }
            })
        }
    }

    private fun removeInjectedView() {
        LOG?.debug("{}: Remove injected view", this.hashCode())
        if (injectedView != null) {
            removeView(injectedView)
            injectedView = null
            LOG?.debug("{}: Injected view removed", this.hashCode())
        }
    }

    private fun injectView() {
        LOG?.debug(
            "{}: Inject view for view adapter instance {}",
            this.hashCode(),
            injectedViewAdapterHolder?.viewAdapterInstance?.hashCode()
        )
        removeInjectedView()
        injectedViewAdapterHolder?.viewAdapterInstance?.viewAdapter?.onDestroy()
        injectedViewAdapterHolder?.viewAdapterInstance?.viewAdapter?.onCreateView(container = this)?.let { view ->
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
            LOG?.debug(
                "{}: View injected for view adapter instance {}",
                this.hashCode(),
                injectedViewAdapterHolder?.viewAdapterInstance?.hashCode()
            )
        }
    }

    companion object {
        private val DEBUG = BuildConfig.DEBUG
        private val LOG: Logger? = if (DEBUG) LoggerFactory.getLogger(InjectedViewContainer::class.java) else null
    }

}

/**
 * Internal use only.
 *
 * Holds a [InjectedViewAdapterInstance] and an associated [InjectedViewAdapterConfigurator].
 *
 * The [InjectedViewAdapterConfigurator] will be used to configure the view adapter every time the
 * [InjectedViewAdapter.onCreateView] function has been called.
 */
data class InjectedViewAdapterHolder<T : InjectedViewAdapter>(
    val viewAdapterInstance: InjectedViewAdapterInstance<T>,
    val viewAdapterConfigurator: InjectedViewAdapterConfigurator<T>
) {

    /**
     * Convenience method to configure the [InjectedViewAdapter].
     */
    internal fun configureViewAdapter() {
        viewAdapterConfigurator.onConfigure(viewAdapterInstance.viewAdapter)
    }

}

/**
 * Internal use only.
 *
 * An interface for configuring an [InjectedViewAdapter].
 *
 * Implement this method when creating an [InjectedViewAdapterHolder] to configure the [InjectedViewAdapter] when
 * needed.
 */
fun interface InjectedViewAdapterConfigurator<T: InjectedViewAdapter> {

    /**
     * Called every time after the [InjectedViewAdapter.onCreateView] has been called.
     *
     * This method is called in `onStart` of the [InjectedViewContainer]'s lifecycle observer.
     *
     * IMPORTANT: When this is called the view adapter has lost its previous state and must be completely reconfigured to
     * be in the required state. For example if a button has been disabled and this method was called afterwards
     * then you have to disabled the button again.
     */
    fun onConfigure(injectedViewAdapter: T)
}
