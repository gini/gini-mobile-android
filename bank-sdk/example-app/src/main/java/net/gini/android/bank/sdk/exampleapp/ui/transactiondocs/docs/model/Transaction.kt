package net.gini.android.bank.sdk.exampleapp.ui.transactiondocs.docs.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
 data class Transaction(
    val paymentRecipient: String,
    val paymentPurpose: String,
    val paymentReference: String,
    val amount: String,
    val iban: String,
    val bic: String,
    val timestamp: Long,
    val attachments: List<Attachment>
) : Parcelable
