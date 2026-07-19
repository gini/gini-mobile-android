package net.gini.android.capture.analysis.warning

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.gini.android.capture.R
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WarningBottomSheetViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `resolves the texts for the warning type`() {
        // When
        val viewModel = WarningBottomSheetViewModel(WarningType.DOCUMENT_MARKED_AS_PAID)

        // Then
        assertThat(viewModel.uiState.value.titleRes)
            .isEqualTo(R.string.gc_document_marked_paid_title)
        assertThat(viewModel.uiState.value.descriptionRes)
            .isEqualTo(R.string.gc_document_marked_paid_desc)
    }

    @Test
    fun `has no texts when the warning type is missing`() {
        // When
        val viewModel = WarningBottomSheetViewModel(null)

        // Then
        assertThat(viewModel.uiState.value.titleRes).isNull()
        assertThat(viewModel.uiState.value.descriptionRes).isNull()
    }

    @Test
    fun `cancel emits the cancel and dismiss side effect`() = runTest {
        // Given
        val viewModel = WarningBottomSheetViewModel(WarningType.DOCUMENT_MARKED_AS_PAID)

        // When
        viewModel.onCancelClicked()

        // Then
        assertThat(viewModel.sideEffects.first())
            .isEqualTo(WarningSideEffect.CancelAndDismiss)
    }

    @Test
    fun `proceed emits the proceed and dismiss side effect`() = runTest {
        // Given
        val viewModel = WarningBottomSheetViewModel(WarningType.DOCUMENT_MARKED_AS_PAID)

        // When
        viewModel.onProceedClicked()

        // Then
        assertThat(viewModel.sideEffects.first())
            .isEqualTo(WarningSideEffect.ProceedAndDismiss)
    }
}
