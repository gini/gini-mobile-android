package net.gini.android.core.api;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Alpar Szotyori on 25.10.2018.
 *
 * Copyright (c) 2018 Gini GmbH.
 */

/**
 * Use this class to pass additional information for a document when uploading it to the Gini API.
 *
 * Besides the predefined metadata fields you may also add custom fields.
 */
public class DocumentMetadata {

    @VisibleForTesting
    public static final String HEADER_FIELD_NAME_PREFIX = "X-Document-Metadata-";
    @VisibleForTesting
    public static final String BRANCH_ID_HEADER_FIELD_NAME = HEADER_FIELD_NAME_PREFIX + "BranchId";
    @VisibleForTesting
    public static final String UPLOAD_METADATA_HEADER_FIELD_NAME = HEADER_FIELD_NAME_PREFIX + "Upload";
    @VisibleForTesting
    public static final String PRODUCT_TAG_HEADER_FIELD_NAME = "x-document-metadata-product-tag";


    private final Map<String, String> mMetadataMap = new HashMap<>();
    private CharsetEncoder mAsciiCharsetEncoder;

    /**
     * Create a new instance.
     */
    public DocumentMetadata() {
        try {
            final Charset asciiCharset = Charset.forName("ASCII");
            mAsciiCharsetEncoder = asciiCharset.newEncoder();
        } catch (IllegalArgumentException ignore) {
            // Shouldn't happen
            mAsciiCharsetEncoder = null;
        }
    }

    @VisibleForTesting
    public DocumentMetadata(@Nullable CharsetEncoder charsetEncoder) {
        mAsciiCharsetEncoder = charsetEncoder;
    }

    /**
     * Set a branch identifier to associate the document with a particular branch.
     *
     * @param branchId an identifier as an ASCII compatible string
     * @throws IllegalArgumentException if the branchId string cannot be encoded as ASCII
     */
    public void setBranchId(@NonNull final String branchId) throws IllegalArgumentException {
        if (isASCIIEncodable(branchId)) {
            putMetadata(BRANCH_ID_HEADER_FIELD_NAME, branchId);
        } else {
            throw new IllegalArgumentException("Metadata is not encodable as ASCII: " + branchId);
        }
    }

    /**
     * Set upload metadata to be sent to backend
     *
     * @param uploadMetadata containing info related to the device, file import type etc...
     */
    public void setUploadMetadata(@NonNull final String uploadMetadata) {
        if (isASCIIEncodable(uploadMetadata)) {
            putMetadata(UPLOAD_METADATA_HEADER_FIELD_NAME, uploadMetadata);
        } else {
            throw new IllegalArgumentException("Metadata is not encodable as ASCII: " + uploadMetadata);
        }
    }

    /**
     * Set the product tag to be sent to backend for document submissions.
     *
     * @param productTag identifies which extraction type should be used
     * @throws IllegalArgumentException if the productTag string cannot be encoded as ASCII
     */
    public void setProductTag(@NonNull final String productTag) {
        if (isASCIIEncodable(productTag)) {
            putMetadata(PRODUCT_TAG_HEADER_FIELD_NAME, productTag);
        } else {
            throw new IllegalArgumentException("Metadata is not encodable as ASCII: " + productTag);
        }
    }

    @VisibleForTesting
    public boolean isASCIIEncodable(@NonNull final String string) {
        if (mAsciiCharsetEncoder != null) {
            return mAsciiCharsetEncoder.canEncode(string);
        }
        // If no ASCII encoder (should never happen) then accept everything to not block metadata
        return true;
    }

    /**
     * Add a custom metadata field. If there is already a field with the same name,
     * the previous value will be overwritten with this one.
     *
     * @param name field name as an ASCII compatible string
     * @param value field value as an ASCII compatible string
     * @throws IllegalArgumentException if the name or the value cannot be encoded as ASCII
     */
    public void add(@NonNull final String name, @NonNull final String value)
            throws IllegalArgumentException {
        if (!isASCIIEncodable(name)) {
            throw new IllegalArgumentException("Metadata name is not encodable as ASCII: " + name);
        }
        if (!isASCIIEncodable(value)) {
            throw new IllegalArgumentException(
                    "Metadata value is not encodable as ASCII: " + value);
        }
        final String completeName;
        if (name.startsWith(HEADER_FIELD_NAME_PREFIX)) {
            completeName = name;
        } else {
            completeName = HEADER_FIELD_NAME_PREFIX + name;
        }
        putMetadata(completeName, value);
    }

    /**
     * Provides a copy of the [DocumentMetadata] object
     *
     * @return the copy of the metadata object
     */
    public DocumentMetadata copy() {
        DocumentMetadata copy = new DocumentMetadata();
        mMetadataMap.forEach(copy::putMetadata);
        return copy;
    }

    private void putMetadata(@NonNull final String name, @NonNull final String value) {
        mMetadataMap.put(name, value);
    }

    @NonNull
    @VisibleForTesting
    public Map<String, String> getMetadata() {
        return mMetadataMap;
    }
}
