package net.gini.android.capture.qrengagement

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.navigation.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.di.getGiniCaptureKoin
import net.gini.android.capture.internal.util.ActivityHelper.forcePortraitOrientationOnPhones
import net.gini.android.capture.internal.util.CancelListener
import net.gini.android.capture.internal.util.disallowScreenshots
import net.gini.android.capture.ui.theme.GiniTheme

class QrEngagementFragment : BottomSheetDialogFragment() {

    private val viewModel: QrEngagementViewModel by getGiniCaptureKoin().inject()

    lateinit var cancelListener: CancelListener


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
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                GiniTheme {
                    QrEngagementScreen(
                        viewModel = viewModel,
                        navController = findNavController()
                    )
                }
            }
        }
    }
}
