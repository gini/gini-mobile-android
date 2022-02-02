package net.gini.android.health.sdk.review.error

/**
 * Thrown when a bank was needed but none had been selected.
 */
class NoBankSelected : Throwable("No Bank Selected")

/**
 * Thrown when there was no payment data in the document extractions.
 */
class NoPaymentDataExtracted : Throwable("No payment data extracted.")