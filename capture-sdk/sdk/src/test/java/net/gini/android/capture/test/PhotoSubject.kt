package net.gini.android.capture.test

import com.google.common.truth.FailureMetadata
import com.google.common.truth.Subject
import com.google.common.truth.Subject.Factory
import net.gini.android.capture.internal.camera.photo.JpegByteArraySubject
import net.gini.android.capture.internal.camera.photo.Photo

class PhotoSubject constructor(metadata: FailureMetadata?, actual: Any?) : Subject(metadata, actual) {
    private val mJpegByteArraySubject: JpegByteArraySubject
    private val actual: Photo?

    init {
        isNotNull()
        this.actual = actual as Photo?
        mJpegByteArraySubject = JpegByteArraySubject(metadata, this.actual!!.data)
    }

    fun hasContentIdInUserComment(contentId: String?) {
        val key = "User Comment ContentId"
        if (contentId == null) {
            failWithActual(key, null)
            return
        }
        mJpegByteArraySubject.hasContentIdInUserComment(contentId)
    }

    fun hasRotationDeltaInUserComment(rotationDelta: Int) {
        mJpegByteArraySubject.hasRotationDeltaInUserComment(rotationDelta)
    }

    fun hasSameUserCommentAs(photo: Photo?) {
        val key = "User Comment"
        if (photo == null) {
            failWithActual(key, null)
            return
        }
        mJpegByteArraySubject.hasSameUserCommentAs(photo.data)
    }

    companion object {
        fun photo(): Factory<PhotoSubject, Photo> {
            return Factory { metadata, actual -> PhotoSubject(metadata, actual) }
        }
    }
}