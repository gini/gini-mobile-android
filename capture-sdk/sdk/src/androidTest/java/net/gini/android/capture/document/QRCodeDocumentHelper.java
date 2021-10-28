package net.gini.android.capture.document;

import net.gini.android.capture.internal.qrcode.PaymentQRCodeData;

import androidx.annotation.NonNull;

/**
 * Created by Alpar Szotyori on 12.01.2018.
 *
 * Copyright (c) 2018 Gini GmbH.
 */
public class QRCodeDocumentHelper {

    public static PaymentQRCodeData getPaymentData(@NonNull final QRCodeDocument qrCodeDocument) {
        return qrCodeDocument.getPaymentData();
    }

    private QRCodeDocumentHelper() {
    }
}