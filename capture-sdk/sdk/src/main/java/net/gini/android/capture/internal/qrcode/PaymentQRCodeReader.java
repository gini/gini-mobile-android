package net.gini.android.capture.internal.qrcode;

import android.media.Image;

import net.gini.android.capture.internal.util.Size;

import java.util.List;
import java.util.concurrent.CancellationException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

/**
 * Created by Alpar Szotyori on 08.12.2017.
 *
 * Copyright (c) 2017 Gini GmbH.
 */

/**
 * Internal use only.
 *
 * Reads the first supported QRCode payment data from images.
 * <p>
 * See {@link PaymentQRCodeParser} for supported formats.
 *
 * @suppress
 */
public class PaymentQRCodeReader {

    private final QRCodeDetector mDetector;
    private final QRCodeParser<PaymentQRCodeData> mParser;
    private Listener mListener = new Listener() {
        @Override
        public void onPaymentQRCodeDataAvailable(
                @NonNull final PaymentQRCodeData paymentQRCodeData) {
        }

        @Override
        public void onNonPaymentQRCodeDetected(@NonNull final String qrCodeContent) {
        }

        @Override
        public void onQRCodeReaderFail() {
        }
    };
    private boolean isReleased = false;

    /**
     * Create a new instance which uses the provided {@link QRCodeDetectorTask} to do QRCode
     * detection.
     *
     * @param qrCodeDetectorTask a {@link QRCodeDetectorTask} implementation
     * @return new instance
     */
    public static PaymentQRCodeReader newInstance(
            @NonNull final QRCodeDetectorTask qrCodeDetectorTask) {
        return new PaymentQRCodeReader(
                new QRCodeDetectorImpl(qrCodeDetectorTask),
                new PaymentQRCodeParser());
    }

    private PaymentQRCodeReader(
            @NonNull final QRCodeDetector detector,
            @NonNull final QRCodeParser<PaymentQRCodeData> parser) {
        mDetector = detector;
        mParser = parser;
        mDetector.setListener(new QRCodeDetector.Listener() {
            @Override
            public void onQRCodesDetected(@NonNull final List<String> qrCodes) {
                if (isReleased) {
                    return;
                }
                for (final String qrCodeContent : qrCodes) {
                    try {
                        final PaymentQRCodeData paymentData = mParser.parse(qrCodeContent);
                        mListener.onPaymentQRCodeDataAvailable(paymentData);
                        return;
                    } catch (final IllegalArgumentException ignored) {
                        mListener.onNonPaymentQRCodeDetected(qrCodeContent);
                    }
                }
            }

            @Override
            public void onQRCodeScannerError(Exception e) {

                //TODO: check content of exception if there will be more use cases in the future
                if (!(e instanceof CancellationException)) {
                    mListener.onQRCodeReaderFail();
                }
            }
        });
    }

    @VisibleForTesting
    QRCodeDetector getDetector() {
        return mDetector;
    }

    /**
     * Reads the first supported QRCode payment data from the image.
     *
     * @param image an image byte array
     * @param imageSize size of the image
     * @param rotation rotation to be applied to the image for correct orientation
     */
    public void readFromImage(@NonNull final Image image, @NonNull final Size imageSize,
                              final int rotation, @NonNull final Callback callback) {
        mDetector.detect(image, imageSize, rotation, callback::onReadingFinished);
    }

    /**
     * Reads the first supported QRCode payment data from the image.
     *
     * @param image an image byte array
     * @param imageSize size of the image
     * @param rotation rotation to be applied to the image for correct orientation
     */
    public void readFromByteArray(@NonNull final byte[] image, @NonNull final Size imageSize,
                              final int rotation) {
        mDetector.detect(image, imageSize, rotation);
    }

    /**
     * Release all resources. Detection not possible after this has been called.
     */
    public void release() {
        isReleased = true;
        mDetector.release();
    }

    public void setListener(@Nullable final Listener listener) {
        mListener = listener;
    }

    /**
     * Internal use only.
     *
     * @suppress
     */
    public interface Listener {

        /**
         * Called when a QRCode was found containing a supported payment data format.
         *
         * @param paymentQRCodeData the payment data found on the image
         */
        void onPaymentQRCodeDataAvailable(@NonNull final PaymentQRCodeData paymentQRCodeData);

        /**
         * Called when a QRCode was found without a supported payment data format.
         *
         * @param qrCodeContent the content of the QRCode
         */
        void onNonPaymentQRCodeDetected(@NonNull final String qrCodeContent);

        /**
         * Called when the reader encounters an exception
         *
         */
        void onQRCodeReaderFail();
    }

    /**
     * Internal use only.
     *
     * @suppress
     */
    public interface Callback {

        /**
         * Called when reading finished.
         */
        void onReadingFinished();
    }
}
