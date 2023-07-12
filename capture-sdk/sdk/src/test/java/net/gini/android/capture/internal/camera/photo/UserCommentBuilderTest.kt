package net.gini.android.capture.internal.camera.photo

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UserCommentBuilderTest {

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
            .setEntryPoint("field")
        val userComment = builder.build()
        // Then
        val keys = getListOfKeys(userComment)
        Truth.assertThat(keys).containsExactly(
            Exif.USER_COMMENT_MAKE, Exif.USER_COMMENT_MODEL,
            Exif.USER_COMMENT_PLATFORM, Exif.USER_COMMENT_OS_VERSION,
            Exif.USER_COMMENT_GINI_CAPTURE_VERSION, Exif.USER_COMMENT_CONTENT_ID,
            Exif.USER_COMMENT_ROTATION_DELTA, Exif.USER_COMMENT_DEVICE_ORIENTATION,
            Exif.USER_COMMENT_DEVICE_TYPE, Exif.USER_COMMENT_SOURCE
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
            Exif.USER_COMMENT_IMPORT_METHOD
        ).inOrder()
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