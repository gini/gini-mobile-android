package net.gini.android.core.api.test

import net.gini.android.core.api.GiniApiType

/**
 * [GiniApiType] with realistic media types for wire-level characterization tests.
 *
 * The core api library ships no production [GiniApiType] implementation, so the v1 values here
 * are realistic placeholders only. The media types which production builds actually send are
 * pinned in the bank api library (v1, hardcoded in GiniBankAPIBuilder) and health api library
 * (GiniHealthAPIBuilder.API_VERSION) wire test suites.
 */
class WireTestGiniApiType(
    override val baseUrl: String = "https://api.gini.net/",
    override val giniJsonMediaType: String = "application/vnd.gini.v1+json",
    override val giniPartialMediaType: String = "application/vnd.gini.v1.partial",
    override val giniCompositeJsonMediaType: String = "application/vnd.gini.v1.composite+json"
) : GiniApiType
