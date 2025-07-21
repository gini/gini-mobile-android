package net.gini.android.capture.internal.camera.photo

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth
import net.gini.android.capture.EntryPoint
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.GiniCaptureHelper
import net.gini.android.capture.internal.camera.photo.ExifUserCommentHelper.Companion.getValueForKeyFromUserComment
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
//TODO: remove after upgrading to robolectric to 4.16
@Config(
    maxSdk = 35,
)
class ExifUserCommentBuilderTest {

    @After
    fun teardown() {
        GiniCaptureHelper.setGiniCaptureInstance(null)
    }

    @Test
    @Throws(Exception::class)
    fun `creates user comment with predetermined order of keys`() {
        // Given
        val builder = Exif.userCommentBuilder()
        // When
        builder.setRotationDelta(90)
            .setContentId("asdasd-assd-ssdsa-sdsdss")
            .setAddMake(true)
            .setAddModel(true)
            .setDeviceOrientation("landscape")
            .setDeviceType("tablet")
            .setSource("picker")
        val userComment = builder.build()
        // Then
        val keys = getListOfKeys(userComment)
        Truth.assertThat(keys).containsExactly(
            Exif.USER_COMMENT_MAKE, Exif.USER_COMMENT_MODEL,
            Exif.USER_COMMENT_PLATFORM, Exif.USER_COMMENT_OS_VERSION,
            Exif.USER_COMMENT_GINI_CAPTURE_VERSION, Exif.USER_COMMENT_CONTENT_ID,
            Exif.USER_COMMENT_ROTATION_DELTA, Exif.USER_COMMENT_DEVICE_ORIENTATION,
            Exif.USER_COMMENT_DEVICE_TYPE, Exif.USER_COMMENT_SOURCE, Exif.USER_COMMENT_ENTRY_POINT
        ).inOrder()
    }

    @Test
    @Throws(Exception::class)
    fun `adds ImportMethod if set`() {
        // Given
        val builder = Exif.userCommentBuilder()
        // When
        builder.setRotationDelta(90)
            .setContentId("asdasd-assd-ssdsa-sdsdss")
            .setAddMake(true)
            .setAddModel(true)
            .setDeviceOrientation("landscape")
            .setDeviceType("tablet")
            .setSource("external")
            .setImportMethod("picker")
        val userComment = builder.build()
        // Then
        val keys = getListOfKeys(userComment)
        Truth.assertThat(keys).containsExactly(
            Exif.USER_COMMENT_MAKE, Exif.USER_COMMENT_MODEL,
            Exif.USER_COMMENT_PLATFORM, Exif.USER_COMMENT_OS_VERSION,
            Exif.USER_COMMENT_GINI_CAPTURE_VERSION, Exif.USER_COMMENT_CONTENT_ID,
            Exif.USER_COMMENT_ROTATION_DELTA, Exif.USER_COMMENT_DEVICE_ORIENTATION,
            Exif.USER_COMMENT_DEVICE_TYPE, Exif.USER_COMMENT_SOURCE,
            Exif.USER_COMMENT_IMPORT_METHOD, Exif.USER_COMMENT_ENTRY_POINT
        ).inOrder()
    }

    @Test
    @Throws(Exception::class)
    fun `adds default EntryPoint to user comment if there is no GiniCapture instance`() {
        // Given
        val builder = Exif.userCommentBuilder()
        // When
        builder.setContentId("asdasd-assd-ssdsa-sdsdss")
        val userComment = builder.build()
        // Then
        val entryPoint = getValueForKeyFromUserComment(Exif.USER_COMMENT_ENTRY_POINT, userComment)
        Truth.assertThat(EntryPoint.valueOf(entryPoint.uppercase())).isEqualTo(GiniCapture.Internal.DEFAULT_ENTRY_POINT)
    }

    @Test
    @Throws(Exception::class)
    fun `adds configured EntryPoint to user comment if there is a GiniCapture instance`() {
        // Given
        GiniCapture.newInstance(InstrumentationRegistry.getInstrumentation().context)
            .setEntryPoint(EntryPoint.FIELD)
            .build()
        val builder = Exif.userCommentBuilder()
        // When
        builder.setContentId("asdasd-assd-ssdsa-sdsdss")
        val userComment = builder.build()
        // Then
        val entryPoint = getValueForKeyFromUserComment(Exif.USER_COMMENT_ENTRY_POINT, userComment)
        Truth.assertThat(EntryPoint.valueOf(entryPoint.uppercase())).isEqualTo(EntryPoint.FIELD)
    }

    private fun getListOfKeys(userComment: String): List<String?> {
        val keys: MutableList<String?> = ArrayList()
        val keyValuePairs = userComment.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        for (keyValuePair in keyValuePairs) {
            val keyAndValue = keyValuePair.split("=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (keyAndValue.isNotEmpty()) {
                keys.add(keyAndValue[0])
            }
        }
        return keys
    }
}