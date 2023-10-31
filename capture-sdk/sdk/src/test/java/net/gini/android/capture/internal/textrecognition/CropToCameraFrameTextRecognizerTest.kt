package net.gini.android.capture.internal.textrecognition

import android.graphics.Rect
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertWithMessage
import com.nhaarman.mockitokotlin2.mock
import net.gini.android.capture.internal.textrecognition.test.RecognizedTextFixtures
import net.gini.android.capture.internal.textrecognition.test.TextRecognizerStub
import net.gini.android.capture.internal.util.Size
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Cannot use `@RunWith(JUnitParamsRunner::class)` because android.graphics.Rect needs the implementation provided by Robolectric.
 * Without Robolectric "method not mocked" exceptions are thrown:
 * > `java.lang.RuntimeException: Method contains in android.graphics.Rect not mocked. See https://developer.android.com/r/studio-ui/build/not-mocked for details.`
 */
@RunWith(AndroidJUnit4::class)
internal class CropToCameraFrameTextRecognizerTest {

    /**
     * Cannot use `@Parameters(method = "textCroppingTestParameterValues")`. See class comment above.
     */
    @Test
    fun `processImage() crops text to the camera frame and maintains text layout`() {
        val testParameters = textCroppingTestParameterValues()
        for (testParameter in testParameters) {
            val testDescription = testParameter[0] as String
            val recognizedText = testParameter[1] as RecognizedText
            val expectedText = testParameter[2] as RecognizedText
            val cameraPreviewSize = testParameter[3] as Size?
            val imageSize = testParameter[4] as Size?
            val imageRotation = testParameter[5] as Int
            val cameraFrame = testParameter[6] as Rect?

            // Given
            val textRecognizer = CropToCameraFrameTextRecognizer(TextRecognizerStub(recognizedText))
            cameraPreviewSize?.let { textRecognizer.cameraPreviewSize = it }
            imageSize?.let { textRecognizer.setImageSizeAndRotation(imageSize, imageRotation) }
            cameraFrame?.let { textRecognizer.cameraFrameRect = it }

            // When
            textRecognizer.processImage(mock(), 100, 200, 0, doneCallback = { text ->
                // Then
                assertWithMessage(
                    """
                    Test: $testDescription
                    Camera preview size: $cameraPreviewSize
                    Image size: $imageSize
                    Image rotation: $imageRotation
                    Camera frame: $cameraFrame
                """.trimIndent()
                )
                    .that(text).isEqualTo(expectedText)
            }, cancelledCallback = {
            })
        }
    }

    /**
     * Cannot use `@Parameters(method = "textCroppingTestParameterValues")`. See class comment above.
     */
    @Test
    fun `processByteArray() crops text to the camera frame and maintains text layout`() {
        val testParameters = textCroppingTestParameterValues()
        for (testParameter in testParameters) {
            val testDescription = testParameter[0] as String
            val recognizedText = testParameter[1] as RecognizedText
            val expectedText = testParameter[2] as RecognizedText
            val cameraPreviewSize = testParameter[3] as Size?
            val imageSize = testParameter[4] as Size?
            val imageRotation = testParameter[5] as Int
            val cameraFrame = testParameter[6] as Rect?

            // Given
            val textRecognizer = CropToCameraFrameTextRecognizer(TextRecognizerStub(recognizedText))
            cameraPreviewSize?.let { textRecognizer.cameraPreviewSize = it }
            imageSize?.let { textRecognizer.setImageSizeAndRotation(imageSize, imageRotation) }
            cameraFrame?.let { textRecognizer.cameraFrameRect = it }

            // When
            textRecognizer.processByteArray(ByteArray(0), 100, 200, 0, doneCallback = { text ->
                // Then
                assertWithMessage(
                    """
                    Test: $testDescription
                    Camera preview size: $cameraPreviewSize
                    Image size: $imageSize
                    Image rotation: $imageRotation
                    Camera frame: $cameraFrame
                """.trimIndent()
                )
                    .that(text).isEqualTo(expectedText)
            }, cancelledCallback = {
            })
        }
    }

    private fun textCroppingTestParameterValues(): List<List<Any?>> = listOf(
        // testDescription, recognizedText, expectedText, cameraPreviewSize, imageSize, imageRotation, cameraFrame
        listOf(
            "Don't crop if some sizes are null",
            RecognizedTextFixtures.fixture1,
            RecognizedTextFixtures.fixture1,
            null,
            null,
            0,
            null
        ),
        listOf(
            "Don't crop if some sizes are null",
            RecognizedTextFixtures.fixture1,
            RecognizedTextFixtures.fixture1,
            null,
            null,
            0,
            Rect(0, 0, 1000, 1000)
        ),
        listOf(
            "Don't crop if some sizes are null",
            RecognizedTextFixtures.fixture1,
            RecognizedTextFixtures.fixture1,
            null,
            Size(1000, 1000),
            0,
            null
        ),
        listOf(
            "Don't crop if some sizes are null",
            RecognizedTextFixtures.fixture1,
            RecognizedTextFixtures.fixture1,
            null,
            Size(1000, 1000),
            0,
            Rect(0, 0, 1000, 1000)
        ),
        listOf(
            "Don't crop if some sizes are null",
            RecognizedTextFixtures.fixture1,
            RecognizedTextFixtures.fixture1,
            Size(1000, 1000),
            null,
            0,
            null
        ),
        listOf(
            "Don't crop if some sizes are null",
            RecognizedTextFixtures.fixture1,
            RecognizedTextFixtures.fixture1,
            Size(1000, 1000),
            null,
            0,
            Rect(0, 0, 1000, 1000)
        ),
        listOf(
            "Don't crop if some sizes are null",
            RecognizedTextFixtures.fixture1,
            RecognizedTextFixtures.fixture1,
            Size(1000, 1000),
            Size(1000, 1000),
            0,
            null
        ),
        listOf(
            "Don't crop if the camera frame contains all the text",
            RecognizedTextFixtures.fixture1,
            RecognizedTextFixtures.fixture1,
            Size(1000, 1000),
            Size(1000, 1000),
            0,
            Rect(0, 0, 1000, 1000)
        ),
        listOf(
            "Don't crop if the camera frame contains all the text and the image size is different from the camera preview size",
            RecognizedTextFixtures.fixture2,
            RecognizedTextFixtures.fixture2,
            Size(1000, 1000),
            Size(100, 100),
            0,
            Rect(0, 0, 1000, 1000)
        ),
        listOf(
            "Crop elements which exceed the left edge of the camera frame",
            RecognizedTextFixtures.fixture1,
            fixture1CroppedLeft,
            Size(1000, 1000),
            Size(1000, 1000),
            0,
            Rect(200, 0, 1000, 1000)
        ),
        listOf(
            "Crop elements which exceed the left edge of the camera frame and the image size is different from the camera preview size",
            RecognizedTextFixtures.fixture2,
            fixture2CroppedLeft,
            Size(1000, 1000),
            Size(100, 100),
            0,
            Rect(200, 0, 1000, 1000)
        ),
        listOf(
            "Crop elements which exceed the top edge of the camera frame",
            RecognizedTextFixtures.fixture1,
            fixture1CroppedTop,
            Size(1000, 1000),
            Size(1000, 1000),
            0,
            Rect(0, 140, 1000, 1000)
        ),
        listOf(
            "Crop elements which exceed the top edge of the camera frame and the image size is different from the camera preview size",
            RecognizedTextFixtures.fixture2,
            fixture2CroppedTop,
            Size(1000, 1000),
            Size(100, 100),
            0,
            Rect(0, 60, 1000, 1000)
        ),
        listOf(
            "Crop elements which exceed the right edge of the camera frame",
            RecognizedTextFixtures.fixture1,
            fixture1CroppedRight,
            Size(1000, 1000),
            Size(1000, 1000),
            0,
            Rect(0, 0, 95, 1000)
        ),
        listOf(
            "Crop elements which exceed the right edge of the camera frame and the image size is different from the camera preview size",
            RecognizedTextFixtures.fixture2,
            fixture2CroppedRight,
            Size(1000, 1000),
            Size(100, 100),
            0,
            Rect(0, 0, 110, 1000)
        ),
        listOf(
            "Crop elements which exceed the bottom edge of the camera frame",
            RecognizedTextFixtures.fixture1,
            fixture1CroppedBottom,
            Size(1000, 1000),
            Size(1000, 1000),
            0,
            Rect(0, 0, 1000, 70)
        ),
        listOf(
            "Crop elements which exceed the bottom edge of the camera frame and the image size is different from the camera preview size",
            RecognizedTextFixtures.fixture2,
            fixture2CroppedBottom,
            Size(1000, 1000),
            Size(100, 100),
            0,
            Rect(0, 0, 1000, 70)
        ),
        listOf(
            "Crop elements which exceed the bottom edge of the camera frame and the image is rotated 90 degrees",
            RecognizedTextFixtures.fixture1,
            fixture1CroppedBottom,
            Size(1000, 1000),
            Size(500, 1000),
            90,
            Rect(0, 0, 1000, 140)
        ),
        listOf(
            "Crop elements which exceed the bottom edge of the camera frame with the image rotated by 270 degrees and the image size being different from the camera preview size",
            RecognizedTextFixtures.fixture2,
            fixture2CroppedBottom,
            Size(1000, 1000),
            Size(50, 100),
            270,
            Rect(0, 0, 1000, 140)
        ),
    )

    private val fixture1CroppedLeft = RecognizedText(
        """
            SE
            Bank
        """.trimIndent(),
        blocks = listOf(
            RecognizedTextBlock(
                listOf(
                    RecognizedTextLine(
                        listOf(
                            RecognizedTextElement("SE", Rect(240, 90, 260, 100)),
                        )
                    ),
                )
            ),
            RecognizedTextBlock(
                listOf(
                    RecognizedTextLine(
                        listOf(
                            RecognizedTextElement("Bank", Rect(200, 130, 240, 140)),
                        )
                    ),
                )
            ),
        )
    )

    private val fixture2CroppedLeft = RecognizedText(
        "DE86210700200123010101",
        blocks = listOf(
            RecognizedTextBlock(
                listOf(
                    RecognizedTextLine(
                        listOf(
                            RecognizedTextElement("DE86210700200123010101", Rect(20, 7, 33, 8)),
                        )
                    ),
                )
            ),
        )
    )

    private val fixture1CroppedTop = RecognizedText(
        "Verwendungszweck:",
        blocks = listOf(
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

    private val fixture2CroppedTop = RecognizedText(
        """
        IBAN: DE86210700200123010101
    """.trimIndent(),
        blocks = listOf(
            RecognizedTextBlock(
                listOf(
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

    private val fixture1CroppedRight = RecognizedText(
        "BIC:",
        blocks = listOf(
            RecognizedTextBlock(
                listOf(
                    RecognizedTextLine(
                        listOf(
                            RecognizedTextElement("BIC:", Rect(50, 110, 90, 120)),
                        )
                    ),
                )
            ),
        )
    )


    private val fixture2CroppedRight = RecognizedText(
        "IBAN:",
        blocks = listOf(
            RecognizedTextBlock(
                listOf(
                    RecognizedTextLine(
                        listOf(
                            RecognizedTextElement("IBAN:", Rect(5, 7, 10, 8)),
                        )
                    ),
                )
            ),
        )
    )


    private val fixture1CroppedBottom = RecognizedText(
        "Bankverbindung",
        blocks = listOf(
            RecognizedTextBlock(
                listOf(
                    RecognizedTextLine(
                        listOf(
                            RecognizedTextElement("Bankverbindung", Rect(50, 50, 190, 60)),
                        )
                    ),
                )
            ),
        )
    )


    private val fixture2CroppedBottom = RecognizedText(
        "Bankverbindung",
        blocks = listOf(
            RecognizedTextBlock(
                listOf(
                    RecognizedTextLine(
                        listOf(
                            RecognizedTextElement("Bankverbindung", Rect(5, 5, 19, 6)),
                        )
                    ),
                )
            ),
        )
    )


}