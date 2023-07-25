package net.gini.android.core.api.test

import net.gini.android.core.api.GiniApiType

/**
 * Created by Alpár Szotyori on 21.07.23.
 *
 * Copyright (c) 2023 Gini GmbH.
 */
class MockGiniApiType: GiniApiType {
    override val baseUrl: String
        get() = ""
    override val giniJsonMediaType: String
        get() = ""
    override val giniPartialMediaType: String
        get() = ""
    override val giniCompositeJsonMediaType: String
        get() = ""
}