package net.gini.android.capture.test;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.google.common.truth.FailureMetadata;
import com.google.common.truth.Subject;

import net.gini.android.capture.Document;
import net.gini.android.capture.document.ImageDocument;
import net.gini.android.capture.internal.camera.photo.JpegByteArraySubject;

import org.checkerframework.checker.nullness.qual.Nullable;

import static com.google.common.truth.Fact.fact;
import static com.google.common.truth.Fact.simpleFact;

public class DocumentSubject extends Subject {

    private final JpegByteArraySubject mJpegByteArraySubject;

    public static Factory<DocumentSubject, Document> document() {
        return new Factory<DocumentSubject, Document>() {
            @Override
            public DocumentSubject createSubject(FailureMetadata metadata, Document actual) {
                return new DocumentSubject(metadata, actual);
            }
        };
    }

    private final Document actual;

    protected DocumentSubject(FailureMetadata metadata, @Nullable Object actual) {
        super(metadata, actual);
        isNotNull();
        this.actual = (Document) actual;
        //noinspection ConstantConditions
        mJpegByteArraySubject = new JpegByteArraySubject(metadata, this.actual.getData());
    }

    public void isEqualToDocument(final Document other) {
        if (other == null) {
            failWithActual("is equal to", null);
        }

        //noinspection ConstantConditions - null check done above
        final Bitmap bitmap = BitmapFactory.decodeByteArray(actual.getData(), 0,
                actual.getData().length);
        //noinspection ConstantConditions - null check done above
        final Bitmap otherBitmap = BitmapFactory.decodeByteArray(other.getData(), 0,
                other.getData().length);

        if (!bitmap.sameAs(otherBitmap)) {
            failWithActual(fact("is equal to", other), simpleFact("contain different bitmaps"));
        } else {
            if (actual instanceof ImageDocument && other instanceof ImageDocument) {
                if (((ImageDocument) actual).getRotationForDisplay() != ((ImageDocument) other).getRotationForDisplay()) {
                    failWithActual(fact("is equal to", other), simpleFact("have different rotation"));
                }
            }
        }
    }

    public void hasSameContentIdInUserCommentAs(final Document other) {
        isNotNull();
        final String key = "User Comment ContentId";

        if (other == null) {
            failWithActual(key, null);
            return;
        }

        mJpegByteArraySubject.hasSameContentIdInUserCommentAs(other.getData());
    }

    public void hasRotationDeltaInUserComment(final int rotationDelta) {
        isNotNull();
        mJpegByteArraySubject.hasRotationDeltaInUserComment(rotationDelta);
    }
}
