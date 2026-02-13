package net.gini.android.bank.sdk.exampleapp.ui.transactiondocs.details

import androidx.compose.runtime.Composable
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import net.gini.android.bank.sdk.exampleapp.ui.ConfigurationViewModel
import net.gini.android.bank.sdk.exampleapp.ui.fragment.ComposeFragment
import net.gini.android.capture.internal.util.ContextHelper

@AndroidEntryPoint
class TransactionDetailsFragment : ComposeFragment() {

    private val viewModel: TransactionDetailsViewModel by viewModels()
    private val configurationViewModel: ConfigurationViewModel by viewModels()

    @Composable
    override fun ScreenContent() {
        val isPhoneLandscape =
            !ContextHelper.isPortraitOrTablet(requireContext())
        TransactionDetailsScreen(
            configurationViewModel = configurationViewModel,
            viewModel = viewModel,
            navigateBack = { findNavController().navigateUp() },
            isPhoneLandscape = isPhoneLandscape
        )
    }
}
