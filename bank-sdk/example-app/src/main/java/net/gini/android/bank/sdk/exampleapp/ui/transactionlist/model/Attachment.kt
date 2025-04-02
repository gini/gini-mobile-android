package net.gini.android.bank.sdk.exampleapp.ui.transactionlist.model

import kotlinx.serialization.Serializable

@Serializable
internal data class Attachment(
    val id: String,
    val filename: String,
)
