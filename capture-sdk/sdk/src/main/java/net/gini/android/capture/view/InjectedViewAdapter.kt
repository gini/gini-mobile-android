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