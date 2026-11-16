package net.gini.android.capture.internal.iban

/**
 * Formats an IBAN for display by grouping its characters in blocks of four separated by spaces,
 * e.g. `DE89370400440532013000` becomes `DE89 3704 0044 0532 0130 00`.
 *
 * Existing whitespace is removed first, so an already grouped IBAN is re-grouped consistently.
 * Blocks let long IBANs wrap at block boundaries and let screen readers announce them in chunks.
 */
internal fun formatIbanForDisplay(iban: String): String =
    iban.filterNot { it.isWhitespace() }
        .chunked(IBAN_DISPLAY_BLOCK_SIZE)
        .joinToString(separator = " ")

/**
 * Formats an IBAN for screen readers: every character separated by a space so it is spoken one by
 * one, with a comma between the blocks of four as a short pause, e.g. `DE89370400440532013000`
 * becomes `D E 8 9, 3 7 0 4, 0 0 4 4, 0 5 3 2, 0 1 3 0, 0 0`. Without it TalkBack reads
 * "1000" as "one thousand", which makes two IBANs impossible to compare by ear.
 */
internal fun formatIbanForSpeech(iban: String): String =
    iban.filterNot { it.isWhitespace() }
        .chunked(IBAN_DISPLAY_BLOCK_SIZE)
        .joinToString(separator = ", ") { block -> block.toList().joinToString(separator = " ") }

private const val IBAN_DISPLAY_BLOCK_SIZE = 4
