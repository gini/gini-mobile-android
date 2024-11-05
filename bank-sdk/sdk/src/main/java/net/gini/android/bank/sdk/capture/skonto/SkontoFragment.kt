package net.gini.android.bank.sdk.capture.skonto

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import net.gini.android.bank.sdk.GiniBank
import net.gini.android.bank.sdk.R
import net.gini.android.bank.sdk.capture.skonto.formatter.AmountFormatter
import net.gini.android.bank.sdk.di.getGiniBankKoin
import net.gini.android.bank.sdk.util.disallowScreenshots
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.internal.util.ActivityHelper.forcePortraitOrientationOnPhones
import net.gini.android.capture.internal.util.CancelListener
import net.gini.android.capture.ui.theme.GiniTheme
import net.gini.android.capture.view.InjectedViewAdapterInstance
import org.koin.core.parameter.parametersOf

class SkontoFragment : Fragment() {

    private val args: SkontoFragmentArgs by navArgs<SkontoFragmentArgs>()

    private val viewModel: SkontoFragmentViewModel by getGiniBankKoin().inject {
        parametersOf(args.data)
    }
    private val amountFormatter : AmountFormatter by getGiniBankKoin().inject()

    lateinit var cancelListener: CancelListener

    var skontoFragmentListener: SkontoFragmentListener? = null
        set(value) {
            field = value
        }

    private val isBottomNavigationBarEnabled =
        GiniCapture.getInstance().isBottomNavigationBarEnabled

    private val customBottomNavBarAdapter: InjectedViewAdapterInstance<SkontoNavigationBarBottomAdapter>? =
        GiniBank.skontoNavigationBarBottomAdapterInstance
    private var customBottomNavigationBarView: View? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (GiniCapture.hasInstance() && !GiniCapture.getInstance().allowScreenshots) {
            requireActivity().window.disallowScreenshots()
        }
        forcePortraitOrientationOnPhones(activity)

        if (resources.getBoolean(net.gini.android.capture.R.bool.gc_is_tablet)) {
            requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        customBottomNavigationBarView =
            container?.let { customBottomNavBarAdapter?.viewAdapter?.onCreateView(it) }

        viewModel.setListener(skontoFragmentListener)

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                GiniTheme {
                    SkontoScreenContent(
                        viewModel = viewModel,
                        isBottomNavigationBarEnabled = isBottomNavigationBarEnabled,
                        customBottomNavBarAdapter = customBottomNavBarAdapter,
                        navigateBack = {
                            findNavController()
                                .navigate(SkontoFragmentDirections.toCaptureFragment())
                        },
                        navigateToInvoiceScreen = { documentId, infoTextLines ->
                            findNavController()
                                .navigate(
                                    SkontoFragmentDirections.toInvoicePreviewFragment(
                                        screenTitle = context.getString(R.string.gbs_skonto_invoice_preview_title),
                                        documentId = documentId,
                                        highlightBoxes = args.invoiceHighlights.flatMap { it.getExistBoxes() }
                                            .toTypedArray(),
                                        infoTextLines = infoTextLines.toTypedArray()
                                    )
                                )
                        },
                        navigateToHelp = {
                            findNavController().navigate(SkontoFragmentDirections.toSkontoHelpFragment())
                        },
                        amountFormatter = amountFormatter,
                    )
                }
            }
        }
    }
}
