package net.gini.android.merchant.sdk.util

sealed class DisplayedScreen {
    object Nothing : DisplayedScreen()
    object PaymentComponentBottomSheet : DisplayedScreen()
    object BankSelectionBottomSheet : DisplayedScreen()
    object MoreInformationFragment : DisplayedScreen()
    object ReviewFragment : DisplayedScreen()
}