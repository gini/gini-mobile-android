package net.gini.android.capture.document

import net.gini.android.capture.Document

/**
 * A document consisting of multiple XML documents.
 * This is created to conform to what is already supported for other file types
 */
class XmlMultiPageDocument : GiniCaptureMultiPageDocument<XmlDocument, GiniCaptureDocumentError> {

    companion object {
        @JvmField
        val CREATOR: android.os.Parcelable.Creator<XmlMultiPageDocument> =
            object : android.os.Parcelable.Creator<XmlMultiPageDocument> {
                override fun createFromParcel(parcel: android.os.Parcel): XmlMultiPageDocument {
                    return XmlMultiPageDocument(parcel)
                }

                override fun newArray(size: Int): Array<XmlMultiPageDocument?> {
                    return arrayOfNulls(size)
                }
            }
    }

    constructor(source: Document.Source, importMethod: Document.ImportMethod) : super(
        Document.Type.XML_MULTI_PAGE,
        source,
        importMethod,
        net.gini.android.capture.internal.util.MimeType.APPLICATION_XML.asString(),
        false
    )

    constructor(document: XmlDocument) : super(
        Document.Type.XML_MULTI_PAGE,
        net.gini.android.capture.internal.util.MimeType.APPLICATION_XML.asString(),
        document
    )

    private constructor(parcel: android.os.Parcel) : super(parcel)

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: android.os.Parcel, flags: Int) {
        super.writeToParcel(dest, flags)
    }
}