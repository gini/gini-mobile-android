package net.gini.android.capture.internal.util;

import android.app.Activity;
import android.content.DialogInterface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import net.gini.android.capture.GiniCapture;
/**
 * Created by Alpar Szotyori on 04.02.2019.
 *
 * Copyright (c) 2019 Gini GmbH.
 */

/**
 * Internal use only.
 *
 * @suppress
 */
public final class AlertDialogHelperCompat {

    public static void showAlertDialog(@Nullable final Activity activity,
            @NonNull final String message,
            @NonNull final String positiveButtonTitle,
            @NonNull final DialogInterface.OnClickListener positiveButtonClickListener,
            @Nullable final String negativeButtonTitle,
            @Nullable final DialogInterface.OnClickListener negativeButtonClickListener,
            @Nullable final DialogInterface.OnCancelListener cancelListener) {
        if (activity == null) {
            return;
        }
        final AlertDialog alertDialog = new MaterialAlertDialogBuilder(activity)
                .setMessage(message)
                .setPositiveButton(positiveButtonTitle, positiveButtonClickListener)
                .setNegativeButton(negativeButtonTitle, negativeButtonClickListener)
                .setOnCancelListener(cancelListener)
                .create();
        if (GiniCapture.hasInstance() && !GiniCapture.getInstance().getAllowScreenshots()) {
            if (alertDialog.getWindow() != null) {
                WindowExtensionsKt.disallowScreenshots(alertDialog.getWindow());
            }
        }
        alertDialog.setCanceledOnTouchOutside(false);
        alertDialog.show();
    }

    private AlertDialogHelperCompat() {
    }
}
