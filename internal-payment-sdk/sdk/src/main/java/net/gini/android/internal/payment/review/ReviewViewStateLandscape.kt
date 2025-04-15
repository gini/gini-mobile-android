package net.gini.android.internal.payment.review

import java.io.Serializable
sealed interface ReviewViewStateLandscape: Serializable {
    data object EXPANDED: ReviewViewStateLandscape
    data object COLLAPSED: ReviewViewStateLandscape
}
