package net.gini.android.capture.analysis

import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import net.gini.android.capture.internal.util.ApplicationHelper

/**
 * Internal use only.
 *
 * Connects the [AnalysisViewModel]'s [AnalysisViewModel.uiState] and [AnalysisViewModel.events]
 * flows to the view layer ([AnalysisFragmentImpl]). Collection is scoped to the fragment's view
 * lifecycle, so no state or event reaches the view after the view is destroyed.
 */
internal class AnalysisFragmentViewModelBinder(
    private val fragment: Fragment
) {

    fun bind(viewModel: AnalysisViewModel, view: AnalysisFragmentImpl) {
        val scope = fragment.viewLifecycleOwner.lifecycleScope
        scope.launch {
            viewModel.uiState
                .map { it.scanAnimationVisible to it.isSavingInvoicesLocallyEnabled }
                .distinctUntilChanged()
                .collect { (visible, isSavingInvoicesLocallyEnabled) ->
                    if (visible) {
                        view.showScanAnimation(isSavingInvoicesLocallyEnabled)
                    } else {
                        view.hideScanAnimation()
                    }
                }
        }
        scope.launch {
            viewModel.uiState
                .map { it.hints }
                .filterNotNull()
                .distinctUntilChanged()
                .collect { view.showHints(it) }
        }
        scope.launch {
            viewModel.uiState
                .map { it.pdfInfoPanelVisible }
                .distinctUntilChanged()
                .collect { visible -> if (visible) view.showPdfInfoPanel() }
        }
        scope.launch {
            viewModel.uiState
                .map { it.pdfTitle }
                .filterNotNull()
                .distinctUntilChanged()
                .collect { view.showPdfTitle(it) }
        }
        scope.launch {
            viewModel.uiState
                .map { it.documentRender }
                .filterNotNull()
                .distinctUntilChanged()
                .collect { view.showBitmap(it.bitmap, it.rotationForDisplay) }
        }
        scope.launch {
            viewModel.events.collect { event -> handleEvent(event, viewModel, view) }
        }
    }

    private fun handleEvent(
        event: AnalysisEvent,
        viewModel: AnalysisViewModel,
        view: AnalysisFragmentImpl
    ) {
        when (event) {
            is AnalysisEvent.WaitForViewLayout ->
                view.waitForViewLayout().thenRun {
                    viewModel.onViewLayoutFinished(view.pdfPreviewSize)
                }

            is AnalysisEvent.ShowAlertDialog ->
                view.showAlertDialog(
                    event.message,
                    event.positiveButtonTitle,
                    event.positiveButtonClickListener,
                    event.negativeButtonTitle,
                    event.negativeButtonClickListener,
                    event.cancelListener
                )

            is AnalysisEvent.ShowErrorMessage ->
                view.showError(event.message, event.document)

            is AnalysisEvent.ShowErrorType ->
                view.showError(event.errorType, event.document)

            is AnalysisEvent.ShowAlreadyPaidWarning ->
                view.showAlreadyPaidWarning(event.warningType, event.onProceed)

            is AnalysisEvent.ShowPaymentDueHint ->
                view.showPaymentDueHint(event.dismissListener, event.dueDate)

            is AnalysisEvent.ShowEducation ->
                view.showEducation { viewModel.onEducationCompleted() }

            is AnalysisEvent.ProcessInvoiceSaving ->
                view.processInvoiceSaving()

            is AnalysisEvent.OpenApplicationDetailsSettings ->
                fragment.activity?.let { ApplicationHelper.startApplicationDetailsSettings(it) }

            is AnalysisEvent.NotifyError ->
                view.listener?.onError(event.error)

            is AnalysisEvent.NotifyExtractionsAvailable ->
                view.listener?.onExtractionsAvailable(
                    event.extractions,
                    event.compoundExtractions,
                    event.returnReasons
                )

            is AnalysisEvent.NotifyNoExtractions ->
                view.listener?.onProceedToNoExtractionsScreen(event.document)

            is AnalysisEvent.NotifyPdfAlertDialogCancelled ->
                view.listener?.onDefaultPDFAppAlertDialogCancelled()
        }
    }
}
