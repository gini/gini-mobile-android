package net.gini.android.bank.api;

import net.gini.android.bank.api.models.AmplitudeRoot
import net.gini.android.bank.api.models.Configuration
import net.gini.android.bank.api.models.ExtractionsContainer
import net.gini.android.bank.api.models.ResolvePaymentInput
import net.gini.android.bank.api.models.ResolvedPayment
import net.gini.android.bank.api.requests.ErrorEvent
import net.gini.android.core.api.DocumentManager
import net.gini.android.core.api.Resource
import net.gini.android.core.api.models.PaymentRequest

/**
 * Created by Alpár Szotyori on 25.01.22.
 *
 * Copyright (c) 2022 Gini GmbH.
 */

/**
 * The [BankApiDocumentManager] is a high level API on top of the Gini Bank API. It
 * provides high level methods to handle document and payment request related tasks easily.
 */
class BankApiDocumentManager internal constructor(private val documentRepository: BankApiDocumentRepository) :
    DocumentManager<BankApiDocumentRepository, ExtractionsContainer>(
        documentRepository
    ) {

    /**
     * Mark a [PaymentRequest] as paid.
     *
     * @param requestId id of request
     * @param resolvePaymentInput information of the actual payment
     * @return [Resource] with the [ResolvedPayment] instance or information about the error
     */
    suspend fun resolvePaymentRequest(
        requestId: String,
        resolvePaymentInput: ResolvePaymentInput,
    ): Resource<ResolvedPayment> =
        documentRepository.resolvePaymentRequest(requestId, resolvePaymentInput)

    /**
     * Send error events to the Gini Bank API.
     *
     * @param errorEvent information about the error
     * @return Empty [Resource] or information about the error
     */
    suspend fun logErrorEvent(
        errorEvent: ErrorEvent
    ): Resource<Unit> =
        documentRepository.logErrorEvent(errorEvent)


    suspend fun getConfigurations(): Resource<Configuration> =
        documentRepository.getConfigurations()

    suspend fun sendEvents(amplitudeRoot: AmplitudeRoot): Resource<Unit> =
        documentRepository.sendEvents(amplitudeRoot)
}
