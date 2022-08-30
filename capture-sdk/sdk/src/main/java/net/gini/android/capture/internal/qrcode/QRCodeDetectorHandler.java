package net.gini.android.capture.internal.qrcode;

import android.media.Image;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.gini.android.capture.internal.camera.api.UIExecutor;
import net.gini.android.capture.internal.util.Size;

import java.util.List;

/**
 * Created by Alpar Szotyori on 11.12.2017.
 *
 * Copyright (c) 2017 Gini GmbH.
 */

/**
 * Handler to execute QRCode detection. To be used with a {@link Looper} and enables detection on
 * background threads.
 */
class QRCodeDetectorHandler extends Handler {

    static final int DETECT_QRCODE = 1;
    private final QRCodeDetectorTask mQRCodeDetectorTask;
    private final UIExecutor mUIExecutor;
    private QRCodeDetector.Listener mListener;

    QRCodeDetectorHandler(final Looper looper,
            final QRCodeDetectorTask qrCodeDetectorTask) {
        super(looper);
        mQRCodeDetectorTask = qrCodeDetectorTask;
        mUIExecutor = new UIExecutor();
    }

    @Override
    public void handleMessage(final Message msg) {
        if (msg.what == DETECT_QRCODE) {
            if (mListener == null) {
                return;
            }
            final List<String> qrCodes;
            if (msg.obj instanceof MessageDataForImage) {
                final MessageDataForImage imageData = (MessageDataForImage) msg.obj;
                qrCodes = mQRCodeDetectorTask.detect(imageData.image,
                        imageData.imageSize, imageData.rotation);
            } else if (msg.obj instanceof MessageDataForByteArray) {
                final MessageDataForByteArray imageData = (MessageDataForByteArray) msg.obj;
                qrCodes = mQRCodeDetectorTask.detect(imageData.imageBytes,
                        imageData.imageSize, imageData.rotation);
            } else {
                throw new IllegalStateException("Unknown message class: " + msg.getClass());
            }
            if (!qrCodes.isEmpty()) {
                mUIExecutor.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mListener.onQRCodesDetected(qrCodes);
                    }
                });
            }
            mUIExecutor.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (msg.obj instanceof MessageDataForImage) {
                        final MessageDataForImage imageData = (MessageDataForImage) msg.obj;
                        imageData.callback.onDetectionFinished();
                    }
                }
            });
        } else {
            super.handleMessage(msg);
        }
    }

    void release() {
        mQRCodeDetectorTask.release();
    }

    void setListener(@Nullable final QRCodeDetector.Listener listener) {
        mListener = listener;
    }

    static class MessageDataForImage {

        final Image image;
        final Size imageSize;
        final int rotation;
        final QRCodeDetector.Callback callback;

        MessageDataForImage(final Image image,
                            final Size imageSize, final int rotation, @NonNull final QRCodeDetector.Callback callback) {
            this.image = image;
            this.imageSize = imageSize;
            this.rotation = rotation;
            this.callback = callback;
        }
    }

    static class MessageDataForByteArray {

        final byte[] imageBytes;
        final Size imageSize;
        final int rotation;

        MessageDataForByteArray(final byte[] imageBytes,
                                final Size imageSize, final int rotation) {
            this.imageBytes = imageBytes;
            this.imageSize = imageSize;
            this.rotation = rotation;
        }
    }
}
