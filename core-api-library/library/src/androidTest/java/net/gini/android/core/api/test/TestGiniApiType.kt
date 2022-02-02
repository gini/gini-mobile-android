package net.gini.android.core.api.test

import net.gini.android.core.api.GiniApiType

/**
 * Created by Alpár Szotyori on 24.01.22.
 *
 * Copyright (c) 2022 Gini GmbH.
 */
class TestGiniApiType(
    override val baseUrl: String = "https://api.gini.net/",
    override val giniJsonMediaType: String = "application/vnd.gini.vTest+json",
    override val giniPartialMediaType: String = "application/vnd.gini.vTest.partial",
    override val giniCompositeJsonMediaType: String = "application/vnd.gini.vTest.composite+json"
) : GiniApiType {
}