package net.gini.android.capture.analysis.warning

/**
 * Represents the type of a business document, such as a credit note.
 *
 * Responsibilities:
 * - Encapsulates the supported business document types relevant to the app
 *   (CREDIT_NOTE, UNKNOWN).
 * - Provides a safe conversion from raw string values (via [from]) into one of the enum constants,
 *   handling nulls and unexpected values gracefully.
 * - Offers convenience methods (e.g. [isCreditNote]) to simplify conditional checks in presenters or views.
 */
enum class BusinessDocType {
    CREDIT_NOTE, UNKNOWN;

    companion object {
        @JvmStatic
        fun from(raw: String?): BusinessDocType {
            return when (raw?.trim()) {
                "CreditNote" -> CREDIT_NOTE
                else -> UNKNOWN
            }
        }
    }

    /**
     * Checks if the business doc type indicates that the document is a credit note.
     *
     * @return true if the business doc type is [CREDIT_NOTE], false otherwise.
     */
    fun isCreditNote(): Boolean = this == CREDIT_NOTE

}