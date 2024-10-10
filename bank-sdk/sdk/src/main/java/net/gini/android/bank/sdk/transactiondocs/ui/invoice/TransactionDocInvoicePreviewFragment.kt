package net.gini.android.bank.sdk.transactiondocs.ui.invoice

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import net.gini.android.bank.sdk.di.getGiniBankKoin
import net.gini.android.bank.sdk.util.disallowScreenshots
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.internal.util.ActivityHelper
import net.gini.android.capture.ui.theme.GiniTheme
import org.koin.core.parameter.parametersOf

class TransactionDocInvoicePreviewFragment : Fragment() {

    private val args: TransactionDocInvoicePreviewFragmentArgs by navArgs<TransactionDocInvoicePreviewFragmentArgs>()

    private val viewModel: TransactionDocInvoicePreviewViewModel by getGiniBankKoin().inject {
        parametersOf(args.screenTitle, args.documentId, args.infoTextLines, args.highlightBoxes)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (GiniCapture.hasInstance() && !GiniCapture.getInstance().allowScreenshots) {
            requireActivity().window.disallowScreenshots()
        }
        ActivityHelper.forcePortraitOrientationOnPhones(activity)

        if (resources.getBoolean(net.gini.android.capture.R.bool.gc_is_tablet)) {
            requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {

        val navigateBack = { requireActivity().onBackPressedDispatcher.onBackPressed() }

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                GiniTheme {
                    TransactionDocInvoicePreviewScreen(
                        modifier = Modifier.fillMaxSize(),
                        viewModel = viewModel,
                        navigateBack = navigateBack
                    )
                }
            }
        }
    }

    companion object {
        fun createInstance(args: TransactionDocInvoicePreviewFragmentArgs): TransactionDocInvoicePreviewFragment {
            return TransactionDocInvoicePreviewFragment().apply {
                arguments = args.toBundle()
            }
        }
    }
}
