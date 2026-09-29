package net.gini.android.capture.internal.util

import android.content.Context
import android.media.ExifInterface
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Unit tests for [ExifOrientationReader].
 *
 * Reading a real orientation tag out of a HEIF container needs the platform
 * decoder, so that is covered by the instrumented test. What is pinned here is
 * the degree mapping — it has to agree with
 * [net.gini.android.capture.internal.camera.photo.ExifReader], or a HEIC and a
 * JPEG of the same photo would be rotated differently.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ExifOrientationReaderTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `maps the rotated orientations to clockwise degrees`() {
        assertThat(ExifOrientationReader.toRotationForDisplay(ExifInterface.ORIENTATION_ROTATE_90))
            .isEqualTo(90)
        assertThat(ExifOrientationReader.toRotationForDisplay(ExifInterface.ORIENTATION_ROTATE_180))
            .isEqualTo(180)
        assertThat(ExifOrientationReader.toRotationForDisplay(ExifInterface.ORIENTATION_ROTATE_270))
            .isEqualTo(270)
    }

    @Test
    fun `maps a normal or absent orientation to no rotation`() {
        assertThat(ExifOrientationReader.toRotationForDisplay(ExifInterface.ORIENTATION_NORMAL))
            .isEqualTo(0)
        assertThat(ExifOrientationReader.toRotationForDisplay(ExifInterface.ORIENTATION_UNDEFINED))
            .isEqualTo(0)
    }

    @Test
    fun `treats mirrored orientations as their unmirrored equivalent`() {
        assertThat(ExifOrientationReader.toRotationForDisplay(ExifInterface.ORIENTATION_FLIP_HORIZONTAL))
            .isEqualTo(0)
        assertThat(ExifOrientationReader.toRotationForDisplay(ExifInterface.ORIENTATION_TRANSPOSE))
            .isEqualTo(0)
    }

    @Test
    fun `returns no rotation for a uri that cannot be opened`() {
        val missing = Uri.fromFile(File(context.cacheDir, "gini-does-not-exist.heic"))

        assertThat(ExifOrientationReader.readRotationForDisplay(missing, context)).isEqualTo(0)
    }

    @Test
    fun `returns no rotation for a file without exif data`() {
        val file = File.createTempFile("gini-exif-orientation-test", ".heic", context.cacheDir)
        file.writeBytes(ByteArray(64))

        try {
            assertThat(ExifOrientationReader.readRotationForDisplay(Uri.fromFile(file), context))
                .isEqualTo(0)
        } finally {
            file.delete()
        }
    }
}
