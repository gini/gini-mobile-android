package net.gini.android.capture.internal.network

import java.util.UUID

data class ConfigurationNetworkResult(
    val configuration: Configuration,
    val id: UUID
)