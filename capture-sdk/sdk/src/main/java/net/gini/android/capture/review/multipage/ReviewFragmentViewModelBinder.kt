package net.gini.android.capture.review.multipage

import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import net.gini.android.capture.internal.util.ApplicationHelper

/**
 * Internal use only.
 *
 * Connects the [ReviewViewModel]'s [ReviewViewModel.uiState] and [ReviewViewModel.events] flows
 * to the [MultiPageReviewFragment]. Collection is scoped to the fragment's view lifecycle, so no
 * state or event reaches the view after the view is destroyed.
 */
internal class ReviewFragmentViewModelBinder(
    private val fragment: MultiPageReviewFragment
) {

    fun bind(viewModel: ReviewViewModel) {
        val scope = fragment.viewLifecycleOwner.lifecycleScope
        scope.launch {
            viewModel.uiState
                .map { it.uploadIndicatorVisible }
                .distinctUntilChanged()
                .collect { visible ->
                    if (visible) fragment.showIndicator() else fragment.hideIndicator()
                }
        }
        scope.launch {
            viewModel.uiState
                .map { it.nextButtonEnabled }
                .distinctUntilChanged()
                .collect { fragment.setNextButtonEnabled(it) }
        }
        scope.launch {
            viewModel.events.collect { event -> handleEvent(event) }
        }
    }

    private fun handleEvent(event: ReviewEvent) {
        when (event) {
            is ReviewEvent.ShowAlertDialog ->
                fragment.showAlertDialog(
                    event.message,
                    event.positiveButtonTitle,
                    event.positiveButtonClickListener,
                    event.negativeButtonTitle,
                    event.negativeButtonClickListener,
                    event.cancelListener
                )

            is ReviewEvent.NavigateToError ->
                fragment.navigateToErrorFragment(event.errorType, event.document)

            is ReviewEvent.PageDeleted ->
                fragment.onPageDeleted(
                    event.deletedPosition,
                    event.newPosition,
                    event.wasLastPage
                )

            is ReviewEvent.OpenApplicationDetailsSettings ->
                fragment.activity?.let { ApplicationHelper.startApplicationDetailsSettings(it) }
        }
    }
}
