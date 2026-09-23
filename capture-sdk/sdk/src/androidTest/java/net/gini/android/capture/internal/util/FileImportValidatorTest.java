package net.gini.android.capture.internal.util;

import static com.google.common.truth.Truth.assertThat;

import static net.gini.android.capture.internal.util.FileImportValidator.FILE_SIZE_LIMIT;

import android.net.Uri;

import net.gini.android.capture.test.Helpers;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.RequiresDevice;
import androidx.test.filters.SdkSuppress;

/**
 * Created by Alpar Szotyori on 09.08.2018.
 *
 * Copyright (c) 2018 Gini GmbH.
 */
public class FileImportValidatorTest {

    private static final String PDF = "invoice.pdf";
    private static final String PDF_WITH_PASSWORD = "invoice-password.pdf";
    private static final String HEIC = "invoice.heic";

    private static Uri sPdfContentUri;
    private static Uri sPdfWithPasswordContentUri;
    private static Uri sHeicContentUri;

    @BeforeClass
    public static void setUpClass() throws Exception {
        sPdfContentUri = Helpers.getAssetFileFileContentUri(PDF);
        sPdfWithPasswordContentUri = Helpers.getAssetFileFileContentUri(PDF_WITH_PASSWORD);
        sHeicContentUri = Helpers.getAssetFileFileContentUri(HEIC);
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        Helpers.deleteAssetFileFromContentUri(PDF);
        Helpers.deleteAssetFileFromContentUri(PDF_WITH_PASSWORD);
        Helpers.deleteAssetFileFromContentUri(HEIC);
    }

    @RequiresDevice
    @Test
    public void should_acceptPDF_withOnePage_andWithoutPassword() throws Exception {
        // Given
        final FileImportValidator fileImportValidator = new FileImportValidator(
                ApplicationProvider.getApplicationContext(), FILE_SIZE_LIMIT);
        // When
        final boolean result = fileImportValidator.matchesCriteria(sPdfContentUri);
        // Then
        assertThat(result).isTrue();
    }

    @RequiresDevice
    @Test
    public void should_NotAcceptPDF_withOnePage_andWithPassword() throws Exception {
        // Given
        final FileImportValidator fileImportValidator = new FileImportValidator(
                ApplicationProvider.getApplicationContext(), FILE_SIZE_LIMIT);
        // When
        final boolean result = fileImportValidator.matchesCriteria(sPdfWithPasswordContentUri);
        // Then
        assertThat(result).isFalse();
        assertThat(fileImportValidator.getError()).isEqualTo(
                FileImportValidator.Error.PASSWORD_PROTECTED_PDF);
    }

    @RequiresDevice
    @SdkSuppress(minSdkVersion = 28)
    @Test
    public void should_acceptHEIC_onApi28AndAbove() {
        // Given
        final FileImportValidator fileImportValidator = new FileImportValidator(
                ApplicationProvider.getApplicationContext(), FILE_SIZE_LIMIT);
        // When
        final boolean result = fileImportValidator.matchesCriteria(sHeicContentUri);
        // Then
        assertThat(result).isTrue();
    }

    @RequiresDevice
    @SdkSuppress(minSdkVersion = 28)
    @Test
    public void should_NotAcceptHEIC_whenOverTheSizeLimit() {
        // Given
        final FileImportValidator fileImportValidator = new FileImportValidator(
                ApplicationProvider.getApplicationContext(), 1024);
        // When
        final boolean result = fileImportValidator.matchesCriteria(sHeicContentUri);
        // Then
        assertThat(result).isFalse();
        assertThat(fileImportValidator.getError()).isEqualTo(
                FileImportValidator.Error.SIZE_TOO_LARGE);
    }
}