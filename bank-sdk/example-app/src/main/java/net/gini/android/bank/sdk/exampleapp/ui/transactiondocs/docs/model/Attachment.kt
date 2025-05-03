package net.gini.android.bank.sdk.exampleapp.ui.transactiondocs.docs.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
data class Attachment(
    val id: String,
    val filename: String,
) : Parcelable
