package net.gini.android.capture.internal.qrcode;

import android.media.Image;

import net.gini.android.capture.internal.util.Size;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Created by Alpar Szotyori on 11.12.2017.
 *
 * Copyright (c) 2017 Gini GmbH.
 */

/**
 * Interface for asynchronous detection of QRCodes from images.
 */
interface QRCodeDetector {

    void detect(@NonNull Image image, @NonNull Size imageSize, int rotation, @NonNull final Callback callback);

    void detect(@NonNull byte[] image, @NonNull Size imageSize, int rotation);

    void release();

    void setListener(@Nullable Listener listener);

    interface Listener {

        /**
         * Called when QRCodes were detected.
         *
         * @param qrCodes list of QRCode content strings
         */
        void onQRCodesDetected(@NonNull final List<String> qrCodes);

        /**
         * Called when QRCode scanner failed to initialize or other exception occured
         *
         */
        void onQRCodeScannerError(Exception e);
    }

    interface Callback {

        /**
         * Called when QR code detection has finished.
         */
        void onDetectionFinished();
    }
}


