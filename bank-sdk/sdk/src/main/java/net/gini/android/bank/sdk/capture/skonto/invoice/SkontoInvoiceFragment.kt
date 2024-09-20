package net.gini.android.bank.sdk.capture.skonto.invoice

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import net.gini.android.bank.sdk.di.getGiniBankKoin
import net.gini.android.bank.sdk.util.disallowScreenshots
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.internal.util.ActivityHelper
import net.gini.android.capture.ui.theme.GiniTheme
import org.koin.core.parameter.parametersOf

class SkontoInvoiceFragment : Fragment() {

    private val args: SkontoInvoiceFragmentArgs by navArgs<SkontoInvoiceFragmentArgs>()

    private val viewModel: SkontoInvoiceFragmentViewModel by getGiniBankKoin().inject {
        parametersOf(args.skontoData, args.invoiceHighlights)
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
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                GiniTheme {
                    SkontoInvoiceScreen(
                        modifier = Modifier,
                        viewModel = viewModel,
                        navigateBack = {
                            findNavController().popBackStack()
                        }
                    )
                }
            }
        }
    }
}
