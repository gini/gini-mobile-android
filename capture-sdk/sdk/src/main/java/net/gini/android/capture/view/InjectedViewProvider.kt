package net.gini.android.capture.view

import android.view.View
import android.view.ViewGroup

/**
 * Created by Alpár Szotyori on 13.05.22.
 *
 * Copyright (c) 2022 Gini GmbH.
 */

interface InjectedViewProvider {
    fun getView(container: ViewGroup): View
    fun onDestroy()
}