package net.gini.android.bank.api

import net.gini.android.core.api.GiniApiType

/**
 * Created by Alpár Szotyori on 24.01.22.
 *
 * Copyright (c) 2022 Gini GmbH.
 */
class GiniBankApiType @JvmOverloads constructor(
    private val apiVersion: Int,
    override val baseUrl: String = "https://pay-api.gini.net/",
    override val giniJsonMediaType: String = "application/vnd.gini.v$apiVersion+json",
    override val giniPartialMediaType: String = "application/vnd.gini.v$apiVersion.partial",
    override val giniCompositeJsonMediaType: String = "application/vnd.gini.v$apiVersion.composite+json"
) : GiniApiType {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GiniBankApiType

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