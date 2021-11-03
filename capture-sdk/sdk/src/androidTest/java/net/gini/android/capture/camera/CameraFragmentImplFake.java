package net.gini.android.capture.camera;

import android.app.Activity;

import net.gini.android.capture.document.QRCodeDocument;
import net.gini.android.capture.internal.camera.api.CameraControllerFake;
import net.gini.android.capture.internal.camera.api.CameraInterface;
import net.gini.android.capture.internal.qrcode.PaymentQRCodeData;
import net.gini.android.capture.internal.ui.FragmentImplCallback;

import androidx.annotation.NonNull;

/**
 * Created by Alpar Szotyori on 15.12.2017.
 *
 * Copyright (c) 2017 Gini GmbH.
 */

public class CameraFragmentImplFake extends CameraFragmentImpl {

    private CameraControllerFake mCameraControllerFake;
    private int mHidePaymentDataDetectedPopupDelayMs = 1000;
    private QRCodeDocument mQRCodeDocument;
    private PaymentQRCodeData mPaymentQRCodeData;

    CameraFragmentImplFake(
            @NonNull final FragmentImplCallback fragment) {
        super(fragment);
    }

    @Override
    long getHideQRCodeDetectedPopupDelayMs() {
        return CameraFragmentImpl.DEFAULT_ANIMATION_DURATION + mHidePaymentDataDetectedPopupDelayMs;
    }

    @Override
    long getDifferentQRCodeDetectedPopupDelayMs() {
        return 100;
    }

    @Override
    void analyzeQRCode(final QRCodeDocument qrCodeDocument) {
        mQRCodeDocument = qrCodeDocument;
        super.analyzeQRCode(qrCodeDocument);
    }

    @NonNull
    @Override
    protected CameraInterface createCameraController(final Activity activity) {
        return mCameraControllerFake = new CameraControllerFake();
    }

    public QRCodeDocument getQRCodeDocument() {
        return mQRCodeDocument;
    }

    CameraControllerFake getCameraControllerFake() {
        return mCameraControllerFake;
    }

    @Override
    public void onPaymentQRCodeDataAvailable(@NonNull final PaymentQRCodeData paymentQRCodeData) {
        super.onPaymentQRCodeDataAvailable(paymentQRCodeData);
        mPaymentQRCodeData = paymentQRCodeData;
    }

    public PaymentQRCodeData getPaymentQRCodeData() {
        return mPaymentQRCodeData;
    }
}
