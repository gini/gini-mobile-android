package net.gini.android.capture.internal.textrecognition.test

import android.graphics.Rect
import net.gini.android.capture.internal.textrecognition.RecognizedText
import net.gini.android.capture.internal.textrecognition.RecognizedTextBlock
import net.gini.android.capture.internal.textrecognition.RecognizedTextElement
import net.gini.android.capture.internal.textrecognition.RecognizedTextLine

internal object RecognizedTextFixtures {

    // For image size: 1000 x 1000
    val fixture1 = RecognizedText("""
        Bankverbindung
        IBAN: DE86210700200123010101
        Empfänger: Zalando SE
        BIC: DEUTEDEHH210
        Bank: Deutsche Bank
        Verwendungszweck:
    """.trimIndent(),
        blocks = listOf(
            RecognizedTextBlock(
                listOf(
                    RecognizedTextLine(
                        listOf(
                            RecognizedTextElement("Bankverbindung", Rect(50, 50, 190, 60)),
                        )
                    ),
                    RecognizedTextLine(
                        listOf(
                            RecognizedTextElement("IBAN:", Rect(50, 70, 100, 80)),
                            RecognizedTextElement("DE86210700200123010101", Rect(110, 70, 330, 80)),
                        )
                    ),
                    RecognizedTextLine(
                        listOf(
                            RecognizedTextElement("Empfänger:", Rect(50, 90, 150, 100)),
                            RecognizedTextElement("Zalando", Rect(160, 90, 230, 100)),
                            RecognizedTextElement("SE", Rect(240, 90, 260, 100)),
                        )
                    ),
                )
            ),
            RecognizedTextBlock(
                listOf(
                    RecognizedTextLine(
                        listOf(
                            RecognizedTextElement("BIC:", Rect(50, 110, 90, 120)),
                            RecognizedTextElement("DEUTEDEHH210", Rect(50, 110, 170, 120)),
                        )
                    ),
                )
            ),
            RecognizedTextBlock(
                listOf(
                    RecognizedTextLine(
                        listOf(
                            RecognizedTextElement("Bank:", Rect(50, 130, 100, 140)),
                            RecognizedTextElement("Deutsche", Rect(110, 130, 190, 140)),
                            RecognizedTextElement("Bank", Rect(200, 130, 240, 140)),
                        )
                    ),
                )
            ),
            RecognizedTextBlock(
                listOf(
                    RecognizedTextLine(
                        listOf(
                            RecognizedTextElement("Verwendungszweck:", Rect(50, 150, 170, 160)),
                        )
                    ),
                )
            ),
        )
    )

    // For image size: 100 x 100
    val fixture2 = RecognizedText("""
        Bankverbindung
        IBAN: DE86210700200123010101
    """.trimIndent(),
        blocks = listOf(
            RecognizedTextBlock(
                listOf(
                    RecognizedTextLine(
                        listOf(
                            RecognizedTextElement("Bankverbindung", Rect(5, 5, 19, 6)),
                        )
                    ),
                    RecognizedTextLine(
                        listOf(
                            RecognizedTextElement("IBAN:", Rect(5, 7, 10, 8)),
                            RecognizedTextElement("DE86210700200123010101", Rect(20, 7, 33, 8)),
                        )
                    ),
                )
            ),
        )
    )

}