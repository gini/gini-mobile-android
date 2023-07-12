package net.gini.android.capture.internal.camera.photo

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth
import net.gini.android.capture.Document
import net.gini.android.capture.document.DocumentFactory
import net.gini.android.capture.document.ImageDocument
import net.gini.android.capture.test.Helpers
import net.gini.android.capture.test.PhotoSubject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class PhotoTest {
    @Before
    @Throws(IOException::class)
    fun setup() {
        TEST_JPEG = Helpers.getTestJpeg()
    }

    @After
    fun teardown() {
        TEST_JPEG = null
    }

    @Test
    fun `supports parceling`() {
        // Given
        val photo = PhotoFactory.newPhotoFromJpeg(
            TEST_JPEG, 0, "portrait", "photo",
            Document.Source.newCameraSource()
        ) as MutablePhoto
        // When
        val photoFromParcel = Helpers.doParcelingRoundTrip(photo, MutablePhoto.CREATOR)
        // Then
        Truth.assertThat(photoFromParcel).isEqualTo(photo)
    }

    @Test
    fun `keeps user comment when creating from document`() {
        // Given
        val photo = PhotoFactory.newPhotoFromJpeg(
            TEST_JPEG, 0, "portrait", "photo",
            Document.Source.newCameraSource()
        ) as MutablePhoto
        // When
        val fromDocument = PhotoFactory.newPhotoFromDocument(
            DocumentFactory.newImageDocumentFromPhoto(photo) as ImageDocument
        ) as MutablePhoto
        // Then
        Truth.assertAbout(PhotoSubject.photo()).that(photo).hasSameUserCommentAs(fromDocument)
    }

    @Test
    fun `sets contentId from user comment when creating from document`() {
        // Given
        val photo = PhotoFactory.newPhotoFromJpeg(
            TEST_JPEG, 0, "portrait",
            "photo", Document.Source.newCameraSource()
        ) as MutablePhoto
        // When
        val fromDocument = PhotoFactory.newPhotoFromDocument(
            DocumentFactory.newImageDocumentFromPhoto(photo) as ImageDocument
        ) as MutablePhoto
        // Then
        Truth.assertThat(photo.contentId).isEqualTo(fromDocument.contentId)
    }

    @Test
    fun `sets rotationDelta from user comment when creating from document`() {
        // Given
        val photo = PhotoFactory.newPhotoFromJpeg(
            TEST_JPEG, 0, "portrait", "photo",
            Document.Source.newCameraSource()
        ) as MutablePhoto
        // When
        photo.edit().rotateTo(90).apply()
        val fromDocument = PhotoFactory.newPhotoFromDocument(
            DocumentFactory.newImageDocumentFromPhoto(photo) as ImageDocument
        ) as MutablePhoto
        // Then
        Truth.assertThat(photo.rotationDelta).isEqualTo(fromDocument.rotationDelta)
    }

    @Test
    fun `generates UUID for contentId when created`() {
        // When
        val photo = PhotoFactory.newPhotoFromJpeg(
            TEST_JPEG, 0, "portrait", "photo",
            Document.Source.newCameraSource()
        ) as MutablePhoto
        // Then
        Truth.assertThat(UUID.fromString(photo.contentId)).isNotNull()
    }

    @Test
    fun `generates unique contentIds for each instance`() {
        // Given
        val photo1 = PhotoFactory.newPhotoFromJpeg(
            TEST_JPEG, 0, "portrait", "photo",
            Document.Source.newCameraSource()
        ) as MutablePhoto
        val photo2 = PhotoFactory.newPhotoFromJpeg(
            TEST_JPEG, 0, "portrait", "photo",
            Document.Source.newCameraSource()
        ) as MutablePhoto
        // Then
        Truth.assertThat(photo1.contentId).isNotEqualTo(photo2.contentId)
    }

    @Test
    fun `adds contentId to exif user comment`() {
        // Given
        val photo = PhotoFactory.newPhotoFromJpeg(
            TEST_JPEG, 0, "portrait", "photo",
            Document.Source.newCameraSource()
        ) as MutablePhoto
        // Then
        Truth.assertAbout(PhotoSubject.photo()).that(photo).hasContentIdInUserComment(photo.contentId)
    }

    @Test
    fun `keeps contentId after rotation`() {
        // Given
        val photo = PhotoFactory.newPhotoFromJpeg(
            TEST_JPEG, 0, "portrait", "photo",
            Document.Source.newCameraSource()
        ) as MutablePhoto
        val contentId = photo.contentId
        // When
        photo.edit().rotateTo(90).apply()
        // Then
        Truth.assertAbout(PhotoSubject.photo()).that(photo).hasContentIdInUserComment(contentId)
    }

    @Test
    fun `keeps contentId after compression`() {
        // Given
        val photo = PhotoFactory.newPhotoFromJpeg(
            TEST_JPEG, 0, "portrait", "photo",
            Document.Source.newCameraSource()
        ) as MutablePhoto
        val contentId = photo.contentId
        // When
        photo.edit().compressBy(10).apply()
        // Then
        Truth.assertAbout(PhotoSubject.photo()).that(photo).hasContentIdInUserComment(contentId)
    }

    @Test
    fun `inits rotationDelta when created`() {
        // When
        val photo = PhotoFactory.newPhotoFromJpeg(
            TEST_JPEG, 0, "portrait", "photo",
            Document.Source.newCameraSource()
        ) as MutablePhoto
        // Then
        Truth.assertThat(photo.rotationDelta).isEqualTo(0)
    }

    @Test
    fun `inits rotationDelta when created with non-zero orientation`() {
        // When
        val photo = PhotoFactory.newPhotoFromJpeg(
            TEST_JPEG, 90, "portrait", "photo",
            Document.Source.newCameraSource()
        ) as MutablePhoto
        // Then
        Truth.assertThat(photo.rotationDelta).isEqualTo(0)
    }

    @Test
    fun `adds rotationDelta to exif user comment`() {
        // Given
        val photo = PhotoFactory.newPhotoFromJpeg(
            TEST_JPEG, 0, "portrait", "photo",
            Document.Source.newCameraSource()
        ) as MutablePhoto
        // Then
        Truth.assertAbout(PhotoSubject.photo()).that(photo).hasRotationDeltaInUserComment(0)
    }

    @Test
    fun `updates rotationDelta after CW rotation`() {
        // Given
        val photo = PhotoFactory.newPhotoFromJpeg(
            TEST_JPEG, 0, "portrait", "photo",
            Document.Source.newCameraSource()
        ) as MutablePhoto
        // When
        photo.edit().rotateTo(90).apply()
        // Then
        Truth.assertAbout(PhotoSubject.photo()).that(photo).hasRotationDeltaInUserComment(90)
    }

    @Test
    fun `updates rotationDelta after CCW rotation`() {
        // Given
        val photo = PhotoFactory.newPhotoFromJpeg(
            TEST_JPEG, 0, "portrait", "photo",
            Document.Source.newCameraSource()
        ) as MutablePhoto
        // When
        photo.edit().rotateTo(-90).apply()
        // Then
        Truth.assertAbout(PhotoSubject.photo()).that(photo).hasRotationDeltaInUserComment(270)
    }

    @Test
    fun `normalizes rotationDelta for CW rotation`() {
        // Given
        val photo = PhotoFactory.newPhotoFromJpeg(
            TEST_JPEG, 0, "portrait", "photo",
            Document.Source.newCameraSource()
        ) as MutablePhoto
        // When
        photo.edit().rotateTo(450).apply()
        // Then
        Truth.assertAbout(PhotoSubject.photo()).that(photo).hasRotationDeltaInUserComment(90)
    }

    @Test
    fun `normalizes rotationDelta for CCW rotation`() {
        // Given
        val photo = PhotoFactory.newPhotoFromJpeg(
            TEST_JPEG, 0, "portrait", "photo",
            Document.Source.newCameraSource()
        ) as MutablePhoto
        // When
        photo.edit().rotateTo(-270).apply()
        // Then
        Truth.assertAbout(PhotoSubject.photo()).that(photo).hasRotationDeltaInUserComment(90)
    }

    @Test
    fun `keeps rotationDelta after compression`() {
        // Given
        val photo = PhotoFactory.newPhotoFromJpeg(
            TEST_JPEG, 90, "portrait", "photo",
            Document.Source.newCameraSource()
        ) as MutablePhoto
        // When
        photo.edit().compressBy(50).apply()
        // Then
        Truth.assertAbout(PhotoSubject.photo()).that(photo).hasRotationDeltaInUserComment(0)
    }

    companion object {
        private var TEST_JPEG: ByteArray? = null
    }
}