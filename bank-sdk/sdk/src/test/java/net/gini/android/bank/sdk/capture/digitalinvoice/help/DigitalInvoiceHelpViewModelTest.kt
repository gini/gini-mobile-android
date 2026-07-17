package net.gini.android.bank.sdk.capture.digitalinvoice.help

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DigitalInvoiceHelpViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `assembles help items in display order`() {
        // Given
        val viewModel = DigitalInvoiceHelpViewModel()

        // Then
        assertThat(viewModel.helpItems)
            .containsExactly(HelpItem.DIGITAL_INVOICE, HelpItem.EDIT, HelpItem.SHOP)
            .inOrder()
    }

    @Test
    fun `emits navigate back side effect when back is clicked`() = runTest {
        // Given
        val viewModel = DigitalInvoiceHelpViewModel()

        viewModel.sideEffects.test {
            // When
            viewModel.onBackClicked()

            // Then
            assertThat(awaitItem())
                .isEqualTo(DigitalInvoiceHelpViewModel.SideEffect.NavigateBack)
        }
    }
}
