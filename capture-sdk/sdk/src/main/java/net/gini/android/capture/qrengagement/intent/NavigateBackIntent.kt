package net.gini.android.capture.qrengagement.intent

import net.gini.android.capture.qrengagement.QrEngagementSideEffect
import net.gini.android.capture.qrengagement.host.QrEngagementContainerHost

internal class NavigateBackIntent {

    fun QrEngagementContainerHost.run() = intent {
        postSideEffect(QrEngagementSideEffect.NavigateBack)
    }
}
