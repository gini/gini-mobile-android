package net.gini.android.capture.test;

import com.google.common.truth.FailureMetadata;
import com.google.common.truth.Subject;

import net.gini.android.capture.internal.camera.photo.JpegByteArraySubject;
import net.gini.android.capture.internal.camera.photo.Photo;

import androidx.annotation.Nullable;

public class PhotoSubject extends Subject {

    private final JpegByteArraySubject mJpegByteArraySubject;

    public static Factory<PhotoSubject, Photo> photo() {
        return PhotoSubject::new;
    }

    private final Photo actual;

    protected PhotoSubject(FailureMetadata metadata, @org.checkerframework.checker.nullness.qual.Nullable Object actual) {
        super(metadata, actual);
        isNotNull();
        this.actual = (Photo) actual;
        //noinspection ConstantConditions
        mJpegByteArraySubject = new JpegByteArraySubject(metadata, this.actual.getData());
    }

    public void hasContentIdInUserComment(final String contentId) {
        final String key = "User Comment ContentId";

        if (contentId == null) {
            failWithActual(key, null);
            return;
        }

        mJpegByteArraySubject.hasContentIdInUserComment(contentId);
    }

    public void hasRotationDeltaInUserComment(final int rotationDelta) {
        mJpegByteArraySubject.hasRotationDeltaInUserComment(rotationDelta);
    }

    public void hasSameUserCommentAs(@Nullable final Photo photo) {
        final String key = "User Comment";
        if (photo == null) {
            failWithActual(key, null);
            return;
        }
        mJpegByteArraySubject.hasSameUserCommentAs(photo.getData());
    }
}
