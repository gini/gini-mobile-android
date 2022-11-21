package net.gini.android.core.api

/**
 * Created by Alpár Szotyori on 24.01.22.
 *
 * Copyright (c) 2022 Gini GmbH.
 */

/**
 * Internal use only.
 */
interface GiniApiType {
    val baseUrl: String
    val giniJsonMediaType: String
    val giniPartialMediaType: String
    val giniCompositeJsonMediaType: String
}