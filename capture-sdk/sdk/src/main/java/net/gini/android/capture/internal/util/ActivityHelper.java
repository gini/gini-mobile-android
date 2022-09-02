package net.gini.android.capture.internal.util;

import static net.gini.android.capture.internal.util.ContextHelper.isTablet;
import static net.gini.android.capture.tracking.EventTrackingHelper.trackAnalysisScreenEvent;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import net.gini.android.capture.tracking.AnalysisScreenEvent;

/**
 * Internal use only.
 *
 * @suppress
 */
public final class ActivityHelper {

    public static void enableHomeAsUp(final AppCompatActivity activity) {
        final ActionBar actionBar = activity.getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    public static <T> void setActivityExtra(
            final Intent target, final String extraKey, final Context context,
            final Class<T> activityClass) {
        target.putExtra(extraKey, new Intent(context, activityClass));
    }

    @SuppressLint("SourceLockedOrientationActivity")
    public static void lockToPortraitOrientation(@Nullable final Activity activity) {
        if (activity == null) {
            return;
        }
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    public static void forcePortraitOrientationOnPhones(@Nullable final Activity activity) {
        if (activity == null) {
            return;
        }
        if (!isTablet(activity)) {
            lockToPortraitOrientation(activity);
        }
    }

    /**
     * Intercepts the back button pressed event once and then disables the {@link OnBackPressedCallback}.
     *
     * Always calls {@link Activity#onBackPressed()} after disabling the {@link OnBackPressedCallback}.
     *
     * @param activity
     * @param callback
     */
    public static void interceptOnBackPressed(@Nullable final AppCompatActivity activity, @NonNull final OnBackPressedCallback callback) {
        if (activity == null) {
            return;
        }
        activity.getOnBackPressedDispatcher().addCallback(activity, new OnBackPressedCallback(callback.isEnabled()) {
            @Override
            public void handleOnBackPressed() {
                callback.handleOnBackPressed();
                callback.setEnabled(false);
                setEnabled(false);
                activity.onBackPressed();
            }
        });
    }

    private ActivityHelper() {
    }
}
