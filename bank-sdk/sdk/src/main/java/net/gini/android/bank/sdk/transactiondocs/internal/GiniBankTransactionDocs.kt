package net.gini.android.bank.sdk.transactiondocs.internal

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import net.gini.android.bank.sdk.di.getGiniBankKoin
import net.gini.android.bank.sdk.transactiondocs.GiniTransactionDocs
import net.gini.android.bank.sdk.transactiondocs.internal.repository.GiniAttachTransactionDocDialogDecisionRepository
import net.gini.android.bank.sdk.transactiondocs.internal.usecase.GetTransactionDocsFeatureEnabledUseCase
import net.gini.android.bank.sdk.transactiondocs.model.extractions.GiniTransactionDoc
import net.gini.android.capture.analysis.transactiondoc.AttachedToTransactionDocumentProvider

internal class GiniBankTransactionDocs internal constructor(
    override val transactionDocsSettings: GiniTransactionDocsSettings =
        getGiniBankKoin().get(),
    private val attachedToTransactionDocumentProvider: AttachedToTransactionDocumentProvider =
        getGiniBankKoin().get(),
    private val attachTransactionDocDialogDecisionRepository: GiniAttachTransactionDocDialogDecisionRepository =
        getGiniBankKoin().get(),
    private val getTransactionDocsFeatureEnabledUseCase: GetTransactionDocsFeatureEnabledUseCase =
        getGiniBankKoin().get()
) : GiniTransactionDocs {

    @Suppress("UnusedParameter")
    fun deleteDocument(document: GiniTransactionDoc) {
        attachedToTransactionDocumentProvider.clear()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override val giniTransactionDocsFlow: Flow<List<GiniTransactionDoc>>
        get() = flowOf(attachTransactionDocDialogDecisionRepository.getAttachDocToTransaction())
            .flatMapLatest { docShouldBeAttached ->
                if ((docShouldBeAttached || transactionDocsSettings.getAlwaysAttachSetting()
                        .first()) && getTransactionDocsFeatureEnabledUseCase()
                ) {
                    attachedToTransactionDocumentProvider.data
                } else {
                    flowOf()
                }
            }.map {
                listOfNotNull(it?.let {
                    GiniTransactionDoc(
                        giniApiDocumentId = it.giniApiDocumentId,
                        documentFileName = it.filename
                    )
                })
            }
}
