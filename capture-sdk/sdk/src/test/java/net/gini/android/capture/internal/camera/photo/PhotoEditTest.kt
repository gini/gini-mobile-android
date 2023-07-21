package net.gini.android.capture.internal.camera.photo

import android.os.Handler
import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth
import net.gini.android.capture.Document
import net.gini.android.capture.document.ImageDocument
import net.gini.android.capture.internal.camera.photo.PhotoEdit.PhotoEditCallback
import net.gini.android.capture.test.Helpers
import net.gini.android.capture.test.PhotoSubject
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class PhotoEditTest {

    @Test
    @Throws(Exception::class)
    fun `allows adding modifications after async apply`() {
        // Given
        val latch = CountDownLatch(1)
        val photo = photo
        val photoEdit = PhotoEdit(photo)

        // When
        // Start async rotations to 90 degrees
        for (i in 0..19) {
            photoEdit.rotateTo(90)
        }
        photoEdit.applyAsync(object : PhotoEditCallback {
            override fun onDone(photo: Photo) {
                latch.countDown()
            }

            override fun onFailed() {}
        })
        // Add rotations until the async apply completes
        val handler = Handler(Looper.getMainLooper())
        handler.post(object : Runnable {
            override fun run() {
                photoEdit.rotateTo(180)
                if (latch.count == 1L) {
                    handler.post(this)
                }
            }
        })

        // Then
        latch.await(500, TimeUnit.MILLISECONDS)
        // Rotation should be at 90 degrees
        Truth.assertAbout(PhotoSubject.photo()).that(photo).hasRotationDeltaInUserComment(90)
    }

    @get:Throws(IOException::class)
    private val photo: Photo
        get() {
            val jpeg = Helpers.getTestJpeg()
            return PhotoFactory.newPhotoFromJpeg(jpeg, 0, "portrait", "phone", Document.Source.newCameraSource())
        }

    @Test
    @Throws(Exception::class)
    fun `allows only one compression modifier`() {
        // Given
        val photo = photo
        val photoEdit = PhotoEdit(photo)
        // When
        photoEdit.compressBy(30)
            .compressBy(50)
            .compressBy(100)
        // Then
        Truth.assertThat(photoEdit.mPhotoModifiers).hasSize(1)
        Truth.assertThat(
            (photoEdit.mPhotoModifiers[0] as PhotoCompressionModifier)
                .quality
        ).isEqualTo(100)
    }
}