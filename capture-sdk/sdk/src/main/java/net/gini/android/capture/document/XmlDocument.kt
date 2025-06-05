package net.gini.android.capture.document

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import net.gini.android.capture.Document
import net.gini.android.capture.internal.util.MimeType
import net.gini.android.capture.logging.ErrorLog
import net.gini.android.capture.logging.ErrorLogger
import net.gini.android.capture.util.IntentHelper
import net.gini.android.capture.util.UriHelper
import org.jetbrains.annotations.VisibleForTesting


/**
 * A document containing an XML file.
 */
class XmlDocument : GiniCaptureDocument {

    var filename: String? = null
        private set

    companion object {
        @JvmStatic
        fun fromIntent(intent: Intent, importMethod: Document.ImportMethod): XmlDocument {
            val uri = IntentHelper.getUri(intent)
                ?: throw IllegalArgumentException("Intent data must contain a Uri")
            return XmlDocument(intent, uri, Document.Source.newExternalSource(), importMethod)
        }

        @JvmField
        val CREATOR: Parcelable.Creator<XmlDocument> = object : Parcelable.Creator<XmlDocument> {
            override fun createFromParcel(parcel: Parcel): XmlDocument {
                return XmlDocument(parcel)
            }

            override fun newArray(size: Int): Array<XmlDocument?> {
                return arrayOfNulls(size)
            }
        }
    }

    @VisibleForTesting
    constructor(
        intent: Intent,
        uri: Uri,
        source: Document.Source,
        importMethod: Document.ImportMethod
    ) : super(
        Document.Type.XML,
        source,
        importMethod,
        MimeType.APPLICATION_XML.asString(),
        null,
        intent,
        uri,
        false
    )

    fun loadFilename(context: Context) {
        val uri = getUri()
        filename = if (uri == null) {
            null
        } else {
            try {
                UriHelper.getFilenameFromUri(uri, context)
            } catch (e: IllegalStateException) {
                ErrorLogger.log(ErrorLog(description = "Failed to get xml filename", exception = e))
                null
            } catch (e: SecurityException) {
                ErrorLogger.log(ErrorLog(description = "Failed to get xml filename", exception = e))
                null
            }
        }
    }

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        super.writeToParcel(dest, flags)
        dest.writeString(filename)
    }

    private constructor(parcel: Parcel) : super(parcel) {
        filename = parcel.readString()
    }
}