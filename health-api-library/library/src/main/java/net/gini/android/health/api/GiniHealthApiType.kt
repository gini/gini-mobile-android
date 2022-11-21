package net.gini.android.health.api

import net.gini.android.core.api.GiniApiType

/**
 * Created by Alpár Szotyori on 24.01.22.
 *
 * Copyright (c) 2022 Gini GmbH.
 */

/**
 * Internal use only.
 */
class GiniHealthApiType @JvmOverloads constructor(
    val apiVersion: Int,
    override val baseUrl: String = "https://health-api.gini.net/",
    override val giniJsonMediaType: String = "application/vnd.gini.v$apiVersion+json",
    override val giniPartialMediaType: String = "application/vnd.gini.v$apiVersion.partial",
    override val giniCompositeJsonMediaType: String = "application/vnd.gini.v$apiVersion.composite+json"
) : GiniApiType {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GiniHealthApiType

        if (apiVersion != other.apiVersion) return false
        if (baseUrl != other.baseUrl) return false
        if (giniJsonMediaType != other.giniJsonMediaType) return false
        if (giniPartialMediaType != other.giniPartialMediaType) return false
        if (giniCompositeJsonMediaType != other.giniCompositeJsonMediaType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = apiVersion
        result = 31 * result + baseUrl.hashCode()
        result = 31 * result + giniJsonMediaType.hashCode()
        result = 31 * result + giniPartialMediaType.hashCode()
        result = 31 * result + giniCompositeJsonMediaType.hashCode()
        return result
    }
}
