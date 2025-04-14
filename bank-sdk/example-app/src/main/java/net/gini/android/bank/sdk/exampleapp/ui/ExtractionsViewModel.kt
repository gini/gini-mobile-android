package net.gini.android.bank.sdk.exampleapp.ui

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import net.gini.android.bank.sdk.exampleapp.ui.extractions.ExtractionsContainerHost
import net.gini.android.bank.sdk.exampleapp.ui.extractions.intent.SaveTransactionDataIntent
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.viewmodel.container
import javax.inject.Inject

@HiltViewModel
internal class ExtractionsViewModel @Inject constructor(
    private val saveTransactionDataIntent: SaveTransactionDataIntent,
) : ViewModel(), ExtractionsContainerHost {

    override val container: Container<Unit, Nothing> = container(Unit)

    fun saveTransactionData(
        iban: String,
        bic: String,
        amountToPay: String,
        paymentRecipient: String,
        paymentPurpose: String,
        paymentReference: String,
    ) = with(saveTransactionDataIntent) {
        run(iban, bic, amountToPay, paymentRecipient, paymentPurpose, paymentReference)
    }
}
