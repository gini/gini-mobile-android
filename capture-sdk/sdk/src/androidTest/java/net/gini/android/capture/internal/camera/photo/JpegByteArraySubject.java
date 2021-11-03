package net.gini.android.capture.internal.camera.photo;

import com.google.common.truth.FailureMetadata;
import com.google.common.truth.Subject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static com.google.common.truth.Fact.fact;
import static com.google.common.truth.Fact.simpleFact;

public class JpegByteArraySubject extends Subject {

    static Factory<JpegByteArraySubject, byte[]> jpegByteArray() {
        return new Factory<JpegByteArraySubject, byte[]>() {

            @Override
            public JpegByteArraySubject createSubject(FailureMetadata metadata, byte[] actual) {
                return new JpegByteArraySubject(metadata, actual);
            }
        };
    }

    private final byte[] actual;

    public JpegByteArraySubject(FailureMetadata metadata, @org.checkerframework.checker.nullness.qual.Nullable Object actual) {
        super(metadata, actual);
        isNotNull();
        this.actual = (byte[]) actual;
    }

    public void hasContentIdInUserComment(@Nullable final String contentId) {
        isNotNull();
        final String key = "User Comment ContentId";

        if (contentId == null) {
            failWithoutActual(fact(key, null));
            return;
        }

        final String userComment = readExifUserComment(key, contentId, actual);
        final String contentIdInUserComment = getValueForKeyfromUserComment("ContentId",
                userComment);

        if (contentIdInUserComment == null) {
            failWithoutActual(simpleFact(String.format("Expected ContentID %s but User Comment had no ContentID", contentId)));
            return;
        }

        if (!contentId.equals(contentIdInUserComment)) {
            failWithoutActual(fact(key, contentId), fact("was", contentIdInUserComment));
        }
    }

    @NonNull
    private String readExifUserComment(@NonNull final String key, @NonNull final String expected,
            @NonNull final byte[] jpeg) {
        try {
            final ExifReader reader = ExifReader.forJpeg(jpeg);
            return reader.getUserComment();
        } catch (final ExifReaderException e) {
            failWithoutActual(simpleFact(String.format("Could not read %s '%s' from <%s> due to error: %s", key, expected, jpeg,
                    e.getMessage())));
        }
        return "";
    }

    @NonNull
    private String readExifUserComment(@NonNull final byte[] jpeg) {
        try {
            final ExifReader reader = ExifReader.forJpeg(jpeg);
            return reader.getUserComment();
        } catch (final ExifReaderException e) {
            failWithoutActual(simpleFact(String.format("Could not read User Comment from <%s> due to error: %s", jpeg,
                    e.getMessage())));
        }
        return "";
    }

    @Nullable
    private String getValueForKeyfromUserComment(@NonNull final String key,
            @NonNull final String userComment) {
        final String[] keyValuePairs = userComment.split(",");
        for (final String keyValuePair : keyValuePairs) {
            final String[] keyAndValue = keyValuePair.split("=");
            if (keyAndValue[0].equals(key)) {
                return keyAndValue[1];
            }
        }
        return null;
    }

    public void hasSameContentIdInUserCommentAs(@Nullable final byte[] jpeg) {
        isNotNull();
        final String key = "User Comment ContentId";

        if (jpeg == null) {
            failWithoutActual(fact(key, null));
            return;
        }

        final String userComment = readExifUserComment(actual);
        final String expectedUserComment = readExifUserComment(jpeg);
        final String subjectUuid = getValueForKeyfromUserComment("ContentId", userComment);
        final String otherUuid = getValueForKeyfromUserComment("ContentId", expectedUserComment);

        if (subjectUuid == null && otherUuid != null) {
            failWithoutActual(simpleFact(String.format("Expected ContentID %s but User Comment had no ContentID", otherUuid)));
            return;
        }
        if (otherUuid == null) {
            failWithoutActual(simpleFact("Target had no ContentId in User Comment"));
            return;
        }

        if (!subjectUuid.equals(otherUuid)) {
            failWithoutActual(fact(key, subjectUuid), fact("was", otherUuid));
        }
    }

    public void hasRotationDeltaInUserComment(final int rotationDelta) {
        isNotNull();
        final String key = "User Comment rotation delta";

        final String userComment = readExifUserComment(actual);
        final String subjectRotDeltaDegString = getValueForKeyfromUserComment("RotDeltaDeg",
                userComment);

        if (subjectRotDeltaDegString == null) {
            failWithoutActual(simpleFact(String.format("Expected rotation delta %s but User Comment had no rotation delta", rotationDelta)));
            return;
        }

        final int subjectRotDeltaDeg = Integer.parseInt(subjectRotDeltaDegString);
        if (subjectRotDeltaDeg != rotationDelta) {
            failWithoutActual(fact(key, rotationDelta), fact("was", subjectRotDeltaDeg));
        }
    }

    public void hasSameUserCommentAs(@Nullable final byte[] other) {
        isNotNull();
        final String key = "User Comment";

        if (other == null) {
            failWithoutActual(fact(key, null));
            return;
        }

        final String subjectUserComment = readExifUserComment(actual);
        final String otherUserComment = readExifUserComment(other);

        if (!otherUserComment.equals(subjectUserComment)) {
            failWithoutActual(fact(key, otherUserComment), fact("was", subjectUserComment));
        }
    }
}
