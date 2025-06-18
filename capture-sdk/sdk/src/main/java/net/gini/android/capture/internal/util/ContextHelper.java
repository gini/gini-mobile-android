package net.gini.android.capture.internal.util;

import android.content.Context;
import android.content.res.Configuration;

import net.gini.android.capture.R;

import androidx.annotation.NonNull;

/**
 * Internal use only.
 *
 * @suppress
 */
public final class ContextHelper {

    /**
     * Internal use only.
     *
     * @suppress
     */
    public static String getClientApplicationId(@NonNull final Context context) {
        return context.getPackageName();
    }

    /**
     * Internal use only.
     *
     * @suppress
     */
    public static boolean isTablet(@NonNull final Context context) {
        return context.getResources().getBoolean(R.bool.gc_is_tablet);
    }

    /**
     * Internal use only.
     *
     * @suppress
     */
    public static boolean isPortraitOrientation(@NonNull final Context context) {
        return context.getResources().getBoolean(R.bool.gc_is_portrait);
    }

    /**
     * Internal use only.
     *
     * @suppress
     */
    public static boolean isPortraitOrTablet(@NonNull final Context context) {
        return isPortraitOrientation(context) || isTablet(context);
    }

    private ContextHelper() {
    }

    /**
     * This method tells us if the fonts are more OR equal to 150% of scale.
     */
    public static boolean isFontScaled(Context context) {
        return context.getResources().getConfiguration().fontScale >= 1.5;
    }

    public static boolean isDarkTheme(Context context) {
        int currentNightMode = context.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;
        return currentNightMode == Configuration.UI_MODE_NIGHT_YES;
    }
}
