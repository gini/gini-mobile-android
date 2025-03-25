package net.gini.android.capture.qrengagement

import androidx.lifecycle.ViewModel
import net.gini.android.capture.qrengagement.factory.createDefaultQrEngagementState
import net.gini.android.capture.qrengagement.host.QrEngagementContainerHost
import net.gini.android.capture.qrengagement.intent.PreviousPageIntent
import net.gini.android.capture.qrengagement.intent.NavigateBackIntent
import net.gini.android.capture.qrengagement.intent.NextPageIntent
import net.gini.android.capture.qrengagement.intent.SkipIntent
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.viewmodel.container

internal class QrEngagementViewModel(
    private val navigateBackIntent: NavigateBackIntent,
    private val nextPageIntent: NextPageIntent,
    private val skipIntent: SkipIntent,
    private val previousPageIntent: PreviousPageIntent
) : ViewModel(), QrEngagementContainerHost {

    override val container: Container<QrEngagementState, QrEngagementSideEffect> =
        container(createDefaultQrEngagementState())

    fun onNavigateBackClicked() =
        with(navigateBackIntent) { run() }

    fun onSkipClicked() =
        with(skipIntent) { run() }

    fun onNextPageClicked() =
        with(nextPageIntent) { run() }

    fun onPreviousPageClicked() =
        with(previousPageIntent) { run() }
}
