package net.gini.android.capture.internal.camera.photo

import com.google.common.truth.Fact
import com.google.common.truth.FailureMetadata
import com.google.common.truth.Subject
import com.google.common.truth.Subject.Factory

class JpegByteArraySubject(metadata: FailureMetadata?, actual: Any?) : Subject(metadata, actual) {
    private val actual: ByteArray?

    init {
        isNotNull()
        this.actual = actual as ByteArray?
    }

    fun hasContentIdInUserComment(contentId: String?) {
        isNotNull()
        val key = "User Comment ContentId"
        if (contentId == null) {
            failWithoutActual(Fact.fact(key, null))
            return
        }
        val userComment = readExifUserComment(key, contentId, actual!!)
        val contentIdInUserComment = getValueForKeyfromUserComment(
            "ContentId",
            userComment
        )
        if (contentIdInUserComment == null) {
            failWithoutActual(
                Fact.simpleFact(
                    String.format(
                        "Expected ContentID %s but User Comment had no ContentID",
                        contentId
                    )
                )
            )
            return
        }
        if (contentId != contentIdInUserComment) {
            failWithoutActual(Fact.fact(key, contentId), Fact.fact("was", contentIdInUserComment))
        }
    }

    private fun readExifUserComment(
        key: String, expected: String,
        jpeg: ByteArray
    ): String {
        try {
            val reader = ExifReader.forJpeg(jpeg)
            return reader.userComment
        } catch (e: ExifReaderException) {
            failWithoutActual(
                Fact.simpleFact(
                    String.format(
                        "Could not read %s '%s' from <%s> due to error: %s", key, expected, jpeg,
                        e.message
                    )
                )
            )
        }
        return ""
    }

    private fun readExifUserComment(jpeg: ByteArray): String {
        try {
            val reader = ExifReader.forJpeg(jpeg)
            return reader.userComment
        } catch (e: ExifReaderException) {
            failWithoutActual(
                Fact.simpleFact(
                    String.format(
                        "Could not read User Comment from <%s> due to error: %s", jpeg,
                        e.message
                    )
                )
            )
        }
        return ""
    }

    private fun getValueForKeyfromUserComment(
        key: String,
        userComment: String
    ): String? {
        val keyValuePairs = userComment.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        for (keyValuePair in keyValuePairs) {
            val keyAndValue = keyValuePair.split("=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (keyAndValue[0] == key) {
                return keyAndValue[1]
            }
        }
        return null
    }

    fun hasSameContentIdInUserCommentAs(jpeg: ByteArray?) {
        isNotNull()
        val key = "User Comment ContentId"
        if (jpeg == null) {
            failWithoutActual(Fact.fact(key, null))
            return
        }
        val userComment = readExifUserComment(actual!!)
        val expectedUserComment = readExifUserComment(jpeg)
        val subjectUuid = getValueForKeyfromUserComment("ContentId", userComment)
        val otherUuid = getValueForKeyfromUserComment("ContentId", expectedUserComment)
        if (subjectUuid == null && otherUuid != null) {
            failWithoutActual(
                Fact.simpleFact(
                    String.format(
                        "Expected ContentID %s but User Comment had no ContentID",
                        otherUuid
                    )
                )
            )
            return
        }
        if (otherUuid == null) {
            failWithoutActual(Fact.simpleFact("Target had no ContentId in User Comment"))
            return
        }
        if (subjectUuid != otherUuid) {
            failWithoutActual(Fact.fact(key, subjectUuid), Fact.fact("was", otherUuid))
        }
    }

    fun hasRotationDeltaInUserComment(rotationDelta: Int) {
        isNotNull()
        val key = "User Comment rotation delta"
        val userComment = readExifUserComment(actual!!)
        val subjectRotDeltaDegString = getValueForKeyfromUserComment(
            "RotDeltaDeg",
            userComment
        )
        if (subjectRotDeltaDegString == null) {
            failWithoutActual(
                Fact.simpleFact(
                    String.format(
                        "Expected rotation delta %s but User Comment had no rotation delta",
                        rotationDelta
                    )
                )
            )
            return
        }
        val subjectRotDeltaDeg = subjectRotDeltaDegString.toInt()
        if (subjectRotDeltaDeg != rotationDelta) {
            failWithoutActual(Fact.fact(key, rotationDelta), Fact.fact("was", subjectRotDeltaDeg))
        }
    }

    fun hasSameUserCommentAs(other: ByteArray?) {
        isNotNull()
        val key = "User Comment"
        if (other == null) {
            failWithoutActual(Fact.fact(key, null))
            return
        }
        val subjectUserComment = readExifUserComment(actual!!)
        val otherUserComment = readExifUserComment(other)
        if (otherUserComment != subjectUserComment) {
            failWithoutActual(Fact.fact(key, otherUserComment), Fact.fact("was", subjectUserComment))
        }
    }

    companion object {
        fun jpegByteArray(): Factory<JpegByteArraySubject, ByteArray> {
            return Factory { metadata, actual -> JpegByteArraySubject(metadata, actual) }
        }
    }
}