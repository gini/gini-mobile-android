package net.gini.android.capture.qrengagement.host

import net.gini.android.capture.qrengagement.QrEngagementSideEffect
import net.gini.android.capture.qrengagement.QrEngagementState
import org.orbitmvi.orbit.ContainerHost

typealias QrEngagementContainerHost = ContainerHost<QrEngagementState, QrEngagementSideEffect>
