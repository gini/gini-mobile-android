package net.gini.android.bank.sdk.invoice

import androidx.lifecycle.ViewModel
import net.gini.android.bank.sdk.invoice.usecase.LoadInvoiceBitmapsUseCase
import net.gini.android.capture.error.ErrorType
import net.gini.android.capture.internal.network.FailureException
import net.gini.android.capture.network.model.GiniCaptureBox
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsEvent
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsEventTracker
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsScreen
import net.gini.android.capture.tracking.useranalytics.properties.UserAnalyticsEventProperty
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container

internal typealias InvoicePreviewHost = ContainerHost<InvoicePreviewFragmentState, Unit>

internal class InvoicePreviewViewModel(
    private val screenTitle: String,
    private val documentId: String,
    private val highlightBoxes: List<GiniCaptureBox>,
    private val infoTextLines: List<String>,
    private val loadInvoiceBitmapsUseCase: LoadInvoiceBitmapsUseCase,
    private val analyticsTracker: UserAnalyticsEventTracker,
) : ViewModel(), InvoicePreviewHost {

    override val container: Container<InvoicePreviewFragmentState, Unit> = container(
        createInitalState()
    )

    private fun createInitalState() =
        InvoicePreviewFragmentState.Ready(
            screenTitle = screenTitle,
            isLoading = true,
            images = emptyList(),
            infoTextLines = infoTextLines,
        )

    init {
        init()
    }

    fun onUserZoomedImage() {
        analyticsTracker.trackEvent(
            UserAnalyticsEvent.PREVIEW_ZOOMED,
            setOf(
                UserAnalyticsEventProperty.Screen(UserAnalyticsScreen.SkontoInvoicePreview)
            )
        )
    }

    fun onUserNavigatesBack() {
        analyticsTracker.trackEvent(
            UserAnalyticsEvent.CLOSE_TAPPED,
            setOf(
                UserAnalyticsEventProperty.Screen(UserAnalyticsScreen.SkontoInvoicePreview)
            )
        )
    }

    internal fun init() = intent {
        analyticsTracker.trackEvent(
            UserAnalyticsEvent.SCREEN_SHOWN,
            setOf(
                UserAnalyticsEventProperty.Screen(UserAnalyticsScreen.SkontoInvoicePreview)
            )
        )

        runCatching {
            reduce { createInitalState() }
            val bitmaps = loadInvoiceBitmapsUseCase.invoke(documentId, highlightBoxes)
            reduce {
                InvoicePreviewFragmentState.Ready(
                    isLoading = false,
                    images = bitmaps,
                    screenTitle = screenTitle,
                    infoTextLines = infoTextLines,
                )
            }
        }.onFailure {
            reduce {
                if (it is FailureException) {
                    InvoicePreviewFragmentState.Error(
                        errorType = it.errorType
                    )
                } else {
                    InvoicePreviewFragmentState.Error(
                        errorType = ErrorType.GENERAL
                    )
                }


            }
        }
    }
}
