package net.gini.android.health.sdk.exampleapp

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import net.gini.android.core.api.Resource
import net.gini.android.health.sdk.GiniHealth
import net.gini.android.health.sdk.exampleapp.pager.PagerAdapter
import net.gini.android.health.sdk.integratedFlow.PaymentFlowConfiguration
import net.gini.android.internal.payment.api.model.PaymentRequest
import net.gini.android.internal.payment.api.model.toPaymentRequest
import net.gini.android.internal.payment.utils.GiniLocalization
import java.io.File

class MainViewModel(
    private val giniHealth: GiniHealth,
) : ViewModel() {
    private val _pages: MutableStateFlow<List<PagerAdapter.Page>> = MutableStateFlow(emptyList())
    val pages: StateFlow<List<PagerAdapter.Page>> = _pages
    private val _paymentRequest = MutableStateFlow<PaymentRequest?>(null)
    val paymentRequest: StateFlow<PaymentRequest?> = _paymentRequest

    private var currentIndex = 0
    private var currentFileUri: Uri? = null
    private var paymentFlowConfiguration: PaymentFlowConfiguration? = null

    fun getNextPageUri(context: Context): Uri {
        val uriForFile = FileProvider.getUriForFile(
            context.applicationContext,
            BuildConfig.APPLICATION_ID + ".fileprovider",
            File(context.filesDir, "page_$currentIndex.jpg")
        )
        currentFileUri = uriForFile
        return uriForFile
    }

    fun onPhotoSaved() {
        currentFileUri?.let { uri ->
            _pages.value = _pages.value.toMutableList().apply { add(PagerAdapter.Page(currentIndex, uri)) }
        }
        ++currentIndex
    }

    fun setDocumentForReview(documentId: String) {
        viewModelScope.launch {
            giniHealth.setDocumentForReview(documentId)
        }
    }

    fun getPaymentRequest(id: String) {
        viewModelScope.launch {
            val response = giniHealth.documentManager.getPaymentRequest(id)

            if (response is Resource.Success) {
                _paymentRequest.value = response.data.toPaymentRequest(id)
            }
        }
    }

    fun setGiniHealthLanguage(localization: GiniLocalization, context: Context) {
        giniHealth.setSDKLanguage(localization, context)
    }

    fun getGiniHealthLanguage(context: Context) = giniHealth.getSDKLanguage(context)

    fun getPaymentFlowConfiguration() = paymentFlowConfiguration

    fun updatePaymentFlowConfiguration(update: PaymentFlowConfiguration.() -> PaymentFlowConfiguration) {
        val currentConfig = paymentFlowConfiguration ?: PaymentFlowConfiguration()
        paymentFlowConfiguration = currentConfig.update()
    }
}
