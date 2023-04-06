package net.gini.android.capture.view

import android.view.View
import android.view.ViewGroup

/**
 * Created by Alpár Szotyori on 13.05.22.
 *
 * Copyright (c) 2022 Gini GmbH.
 */

/**
 * Adapter for injectable views. It allows clients to inject their own views into our layouts.
 */
interface InjectedViewAdapter {
    /**
     * Called when the custom view is required. It will be injected into the SDK's layout.
     *
     * @param container the [ViewGroup] which will contain the returned view
     * @return your custom view
     */
    fun onCreateView(container: ViewGroup): View

    /**
     * Called when the layout is destroyed.
     */
    fun onDestroy()
}

/**
 * Internal use only.
 *
 * Represents an "instance" of an [InjectedViewAdapter] which will be shown in one or more screens' [InjectedViewContainer].
 *
 * We use this class to wrap the [InjectedViewAdapter] and attach additional information needed to manage it.
 *
 * We need to know with which [InjectedViewContainer] the view adapter is associated with to prevent destructive
 * modifications from view containers that are not the currently associated one. For example if the view adapter is
 * shown in a new view container and the previous one is destroyed then the view adapter won't be destroyed because
 * the new associated view container is different than the old one.
 */
class InjectedViewAdapterInstance<T : InjectedViewAdapter> @JvmOverloads constructor(
    val viewAdapter: T,
    var viewContainer: InjectedViewContainer<T>? = null
)