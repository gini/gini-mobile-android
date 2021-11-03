package net.gini.android.capture.component;

import android.content.Context;
import android.content.Intent;

import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.gini.android.capture.GiniCaptureError;

public class CaptureComponentContract extends ActivityResultContract<Intent, CaptureComponentContract.Result> {

    @NonNull
    @Override
    public Intent createIntent(@NonNull Context context, Intent input) {
        return input;
    }

    @Override
    public Result parseResult(int resultCode, @Nullable @org.jetbrains.annotations.Nullable Intent intent) {
        GiniCaptureError error = null;
        if (resultCode == MainActivity.RESULT_ERROR) {
            if (intent != null) {
                error = intent.getParcelableExtra(MainActivity.EXTRA_OUT_ERROR);
            }
        }
        return new Result(resultCode, error);
    }

    public static final class Result {
        private final int resultCode;
        @Nullable private final GiniCaptureError error;

        public Result(final int resultCode, @Nullable final GiniCaptureError error) {
            this.resultCode = resultCode;
            this.error = error;
        }

        public int getResultCode() {
            return resultCode;
        }

        @Nullable
        public GiniCaptureError getError() {
            return error;
        }
    }
}
