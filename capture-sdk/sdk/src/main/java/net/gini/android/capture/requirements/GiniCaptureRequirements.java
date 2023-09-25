package net.gini.android.capture.requirements;

import static net.gini.android.capture.internal.util.ContextHelper.isTablet;

import android.content.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

import androidx.annotation.NonNull;

/**
 * <p>
 *     Checks the device's hardware capabilities to determine, if it is compatible with the Gini Capture SDK.
 * </p>
 * <p>
 *     The checked requirements are listed in the {@link RequirementId} enum.
 * </p>
 * <p>
 *     Call {@link GiniCaptureRequirements#checkRequirements(Context)} to get a report of the requirement checks.
 * </p>
 * <p>
 *     On Android 6.0 and later you need to ask the user for the camera permission before you check the requirements.
 * </p>
 *
 * @deprecated Checking the requirements is no longer necessary and this class will be removed in a future release.
 *             The majority of Android devices already meet the SDK's requirements.
 */
@Deprecated
public final class GiniCaptureRequirements {

    private static final Logger LOG = LoggerFactory.getLogger(GiniCaptureRequirements.class);

    /**
     * <p>
     *     Checks the device's hardware capabilities.
     * </p>
     * @param context any {@link Context} instance
     * @return {@link RequirementsReport} containing information about the checks
     */
    public static RequirementsReport checkRequirements(final Context context) {
        LOG.info("Checking requirements");
        RequirementsReport requirementsReport = checkWithCameraX(context);

        if (!requirementsReport.isFulfilled()) {
            requirementsReport = checkWithOldCameraApi(context);
        }

        return requirementsReport;
    }

    private static RequirementsReport checkWithCameraX(@NonNull final Context context) {
        LOG.info("Checking with CameraX");
        final CameraHolder cameraHolder = new CameraXHolder(context);

        final RequirementsReport requirementsReport = checkRequirements(context, cameraHolder);

        cameraHolder.closeCamera();

        LOG.info("Requirements checked with results: {}", requirementsReport);
        return requirementsReport;
    }

    private static RequirementsReport checkWithOldCameraApi(@NonNull final Context context) {
        LOG.info("Checking with old camera api");
        final CameraHolder cameraHolder = new OldCameraApiHolder();

        final RequirementsReport requirementsReport = checkRequirements(context, cameraHolder);

        cameraHolder.closeCamera();

        LOG.info("Requirements checked with results: {}", requirementsReport);
        return requirementsReport;
    }

    private static RequirementsReport checkRequirements(@NonNull Context context, CameraHolder cameraHolder) {
        final List<Requirement> requirements;
        if (isTablet(context)) {
            requirements = getTabletRequirements(context, cameraHolder);
        } else {
            requirements = getPhoneRequirements(context, cameraHolder);
        }

        return new RequirementsChecker(requirements)
                .checkRequirements();
    }

    @NonNull
    private static List<Requirement> getPhoneRequirements(final Context context,
            final CameraHolder cameraHolder) {
        return Arrays.asList(
                new CameraPermissionRequirement(context),
                new CameraRequirement(cameraHolder),
                new CameraResolutionRequirement(cameraHolder),
                new CameraFlashRequirement(cameraHolder),
                new CameraFocusRequirement(cameraHolder),
                new DeviceMemoryRequirement(cameraHolder)
        );
    }

    @NonNull
    private static List<Requirement> getTabletRequirements(final Context context,
            final CameraHolder cameraHolder) {
        return Arrays.asList(
                new CameraPermissionRequirement(context),
                new CameraRequirement(cameraHolder),
                new CameraResolutionRequirement(cameraHolder),
                new CameraFocusRequirement(cameraHolder),
                new DeviceMemoryRequirement(cameraHolder)
        );
    }

    private GiniCaptureRequirements() {
    }
}
