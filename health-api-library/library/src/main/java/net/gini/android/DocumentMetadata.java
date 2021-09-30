package net.gini.android;

import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

/**
 * Created by Alpar Szotyori on 25.10.2018.
 *
 * Copyright (c) 2018 Gini GmbH.
 */

/**
 * Use this class to pass additional information for a document when uploading it to the Gini API.
 *
 * <p> Besides the predefined metadata fields you may also add custom fields.
 */
public class DocumentMetadata {

    @VisibleForTesting
    static final String HEADER_FIELD_NAME_PREFIX = "X-Document-Metadata-";
    @VisibleForTesting
    static final String BRANCH_ID_HEADER_FIELD_NAME = HEADER_FIELD_NAME_PREFIX + "BranchId";


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
    DocumentMetadata(@Nullable CharsetEncoder charsetEncoder) {
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
            mMetadataMap.put(BRANCH_ID_HEADER_FIELD_NAME, branchId);
        } else {
            throw new IllegalArgumentException("Metadata is not encodable as ASCII: " + branchId);
        }
    }

    @VisibleForTesting
    boolean isASCIIEncodable(@NonNull final String string) {
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
        mMetadataMap.put(completeName, value);
    }

    @NonNull
    Map<String, String> getMetadata() {
        return mMetadataMap;
    }
}
