package net.gini.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.test.filters.SmallTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Map;

/**
 * Created by Alpar Szotyori on 25.10.2018.
 *
 * Copyright (c) 2018 Gini GmbH.
 */

@SmallTest
@RunWith(AndroidJUnit4.class)
public class DocumentMetadataTest {

    private DocumentMetadata mDocumentMetadata;

    @Before
    public void setUp() throws Exception {
        mDocumentMetadata = new DocumentMetadata();
    }

    @Test
    public void testBranchIdFieldName() {
        final String branchId = "4321";
        mDocumentMetadata.setBranchId(branchId);

        assertFalse(mDocumentMetadata.getMetadata().isEmpty());
        boolean containsBranchId = false;
        for (final Map.Entry<String, String> entry : mDocumentMetadata.getMetadata().entrySet()) {
            if (entry.getValue().equals(branchId)) {
                containsBranchId = true;
                final String fieldName = entry.getKey();
                assertTrue(fieldName.startsWith(DocumentMetadata.HEADER_FIELD_NAME_PREFIX));
                assertEquals(DocumentMetadata.BRANCH_ID_HEADER_FIELD_NAME, fieldName);
            }
        }
        assertTrue(containsBranchId);
    }

    @Test
    public void testBranchIdMustBeASCIIEncodable() {
        IllegalArgumentException exception = null;
        try {
            mDocumentMetadata.setBranchId("Bräustüberl");
        } catch (IllegalArgumentException e) {
            exception = e;
        }
        assertNotNull(exception);
    }

    @Test
    public void testCustomFieldNameContainsHeaderPrefix() {
        final String name = "App";
        final String value = "DieBank";
        mDocumentMetadata.add(name, value);

        assertFalse(mDocumentMetadata.getMetadata().isEmpty());
        boolean containsBranchId = false;
        for (final Map.Entry<String, String> entry : mDocumentMetadata.getMetadata().entrySet()) {
            if (entry.getValue().equals(value)) {
                containsBranchId = true;
                assertEquals(DocumentMetadata.HEADER_FIELD_NAME_PREFIX + name, entry.getKey());
            }
        }
        assertTrue(containsBranchId);
    }

    @Test
    public void testCustomFieldNameHeaderPrefixIsNotAddedTwice() {
        final String name = DocumentMetadata.HEADER_FIELD_NAME_PREFIX + "App";
        final String value = "DieBank";
        mDocumentMetadata.add(name, value);

        assertFalse(mDocumentMetadata.getMetadata().isEmpty());
        boolean containsBranchId = false;
        for (final Map.Entry<String, String> entry : mDocumentMetadata.getMetadata().entrySet()) {
            if (entry.getValue().equals(value)) {
                containsBranchId = true;
                assertEquals(name, entry.getKey());
            }
        }
        assertTrue(containsBranchId);
    }

    @Test
    public void testCustomFieldNameMustBeASCIIEncodable() {
        IllegalArgumentException exception = null;
        try {
            mDocumentMetadata.add("Übung", "Rumpfdrehen");
        } catch (IllegalArgumentException e) {
            exception = e;
        }
        assertNotNull(exception);
    }

    @Test
    public void testCustomFieldValueMustBeASCIIEncodable() {
        IllegalArgumentException exception = null;
        try {
            mDocumentMetadata.add("App", "Fotoüberweisung");
        } catch (IllegalArgumentException e) {
            exception = e;
        }
        assertNotNull(exception);
    }

    @Test
    public void testAcceptsAllStringsWhenASCIIEncoderIsNotAvailable() {
        final DocumentMetadata documentMetadata = new DocumentMetadata(null);
        assertTrue(documentMetadata.isASCIIEncodable("Bräustüberl"));
    }

}