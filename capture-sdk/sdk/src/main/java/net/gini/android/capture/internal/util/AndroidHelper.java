package net.gini.android.capture.internal.util;

import android.Manifest;
import android.os.Build;

import java.lang.reflect.Field;

/**
 * Internal use only.
 *
 * @suppress
 */
public final class AndroidHelper {

    /**
     * Internal use only.
     *
     * @suppress
     */
    public static boolean isMarshmallowOrLater() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }

    /**
     * Internal use only
     *
     * Checks whether the current android version is Tiramisu (Android 13, API Level 33) or later using reflection
     * to enable forward compatibility.
     *
     * This method helps you prepare for changes that need to be activated on Android 13 and later without
     * having to set the compile sdk to android api level 33.
     *
     * @return true, if the current android version is Tiramisu (Android 13) or later
     */
    public static boolean isTiramisuOrLater() {
        try {
            final Field tiramisu = Build.VERSION_CODES.class.getField("TIRAMISU");
            final int tiramisuVersionCode = tiramisu.getInt(null);
            return Build.VERSION.SDK_INT >= tiramisuVersionCode;
        } catch (NoSuchFieldException | IllegalAccessException | SecurityException e) {
            return false;
        }
    }

    /**
     * Internal use only.
     *
     * Checks whether a permission is available using reflection to enable forward compatibility.
     *
     * This methods helps you handle permissions that will be available in a later android sdk version without
     * having to bump the compile sdk and target sdk versions in the build.gradle file. Once you are ready to
     * target the newer android sdk no more code changes will be needed.
     *
     * @param permissionName name of the permission as found in the Manifest.permission static class
     * @return true, if the permission is available
     */
    public static boolean isPermissionAvailableOnCurrentAndroidVersion(String permissionName) {
        try {
            Manifest.permission.class.getField(permissionName);
            return true;
        } catch (NoSuchFieldException | SecurityException e) {
            return false;
        }
    }

    /**
     * Internal use only.
     *
     * Gets the fully qualified permission name using reflection to enable forward compatibility.
     *
     * This methods helps you handle permissions that will be available in a later android sdk version without
     * having to bump the compile sdk and target sdk versions in the build.gradle file. Once your app is ready to
     * target the newer android sdk no more code changes will be needed.
     *
     * @param permissionName name of the permission as found in the Manifest.permission static class
     * @return fully qualified permission name or an empty string
     */
    public static String getFullyQualifiedPermissionName(String permissionName) {
        try {
            final Field readMediaImages = Manifest.permission.class.getField(permissionName);
            return (String) readMediaImages.get(null);
        } catch (NoSuchFieldException | IllegalAccessException | SecurityException e) {
            return "";
        }
    }

    private AndroidHelper() {
    }
}
