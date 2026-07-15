package net.gini.android.core.api.test

import net.gini.android.core.api.GiniApiType

/**
 * [GiniApiType] with realistic media types for wire-level characterization tests.
 */
class WireTestGiniApiType(
    override val baseUrl: String = "https://api.gini.net/",
    override val giniJsonMediaType: String = "application/vnd.gini.v1+json",
    override val giniPartialMediaType: String = "application/vnd.gini.v1.partial",
    override val giniCompositeJsonMediaType: String = "application/vnd.gini.v1.composite+json"
) : GiniApiType
