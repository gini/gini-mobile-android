package net.gini.android.capture

/**
 * This class is the container for transferring properties between the Gini Bank SDK and
 * Gini Capture SDK and between the Fragments of the Gini Capture SDK.
 *
 * @param isSkontoSDKFlagEnabled is the Skonto SDK flag inside bank SDK
 * @param isReturnAssistantSDKFlagEnabled is the Return Assistant SDK flag inside bank SDK
 * @param isSkontoExtractionsValid validates if the extraction in the compound extractions
 * contain valid Skonto data
 * @param isReturnAssistantExtractionsValid validates if the extraction in the compound extractions
 * contain valid Return Assistant data
 *
 * Internal use only.
 */
data class BankSDKProperties(
    val isSkontoSDKFlagEnabled: Boolean = false,
    val isReturnAssistantSDKFlagEnabled: Boolean = false,
    val isSkontoExtractionsValid: Boolean = false,
    val isReturnAssistantExtractionsValid: Boolean = false
    )
