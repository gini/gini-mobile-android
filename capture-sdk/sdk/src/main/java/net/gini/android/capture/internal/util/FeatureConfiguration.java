package net.gini.android.capture.internal.util;

import net.gini.android.capture.DocumentImportEnabledFileTypes;
import net.gini.android.capture.GiniCapture;

/**
 * Created by Alpar Szotyori on 05.03.2018.
 *
 * Copyright (c) 2018 Gini GmbH.
 */

/**
 * Internal use only.
 *
 * @suppress
 */
public final class FeatureConfiguration {

    public static DocumentImportEnabledFileTypes getDocumentImportEnabledFileTypes() {
        return GiniCapture.hasInstance()
                ? GiniCapture.getInstance().getDocumentImportEnabledFileTypes()
                : DocumentImportEnabledFileTypes.NONE;
    }

    public static boolean isFileImportEnabled() {
        return GiniCapture.hasInstance() && GiniCapture.getInstance().isFileImportEnabled();
    }

    public static boolean isQRCodeScanningEnabled() {
        return GiniCapture.hasInstance() && GiniCapture.getInstance().isQRCodeScanningEnabled();
    }

    public static boolean shouldShowOnboardingAtFirstRun() {
        return GiniCapture.hasInstance() && GiniCapture.getInstance().shouldShowOnboardingAtFirstRun();
    }

    public static boolean shouldShowOnboarding() {
        return GiniCapture.hasInstance() && GiniCapture.getInstance().shouldShowOnboarding();
    }

    public static boolean isMultiPageEnabled() {
        return GiniCapture.hasInstance() && GiniCapture.getInstance().isMultiPageEnabled();
    }

    private FeatureConfiguration() {
    }
}
