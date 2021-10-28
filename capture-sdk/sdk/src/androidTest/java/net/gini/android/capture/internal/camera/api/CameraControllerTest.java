package net.gini.android.capture.internal.camera.api;

import static com.google.common.truth.Truth.assertThat;

import static net.gini.android.capture.internal.camera.api.SizeSelectionHelper.getBestSize;
import static net.gini.android.capture.test.Helpers.prepareLooper;
import static net.gini.android.capture.test.PermissionsHelper.grantCameraPermission;

import static androidx.test.InstrumentationRegistry.getTargetContext;

import android.content.Intent;
import android.hardware.Camera;

import net.gini.android.capture.internal.util.Size;
import net.gini.android.capture.requirements.CameraResolutionRequirement;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.core.util.Pair;
import androidx.test.core.app.ActivityScenario;
import androidx.test.filters.RequiresDevice;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import jersey.repackaged.jsr166e.CompletableFuture;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

@RunWith(AndroidJUnit4.class)
public class CameraControllerTest {

    private CameraController mCameraController;

    @Before
    public void setUp() throws InterruptedException {
        prepareLooper();
        grantCameraPermission();
    }

    @After
    public void tearDown() throws Exception {
        if (mCameraController != null) {
            mCameraController.close();
            mCameraController = null;
        }
    }

    @Test
    @RequiresDevice
    public void should_useBestPictureResolution() throws InterruptedException {
        openCameraWithCameraController(camera -> {
            final Camera.Parameters parameters = camera.getParameters();
            final Pair<Size, Size> sizes = getBestSize(
                    parameters.getSupportedPictureSizes()
                            .stream()
                            .map(size -> new Size(size.width, size.height))
                            .collect(Collectors.toList()),
                    parameters.getSupportedPreviewSizes()
                            .stream()
                            .map(size -> new Size(size.width, size.height))
                            .collect(Collectors.toList()),
                    CameraResolutionRequirement.MAX_PICTURE_AREA,
                    CameraResolutionRequirement.MIN_PICTURE_AREA,
                    CameraResolutionRequirement.MIN_ASPECT_RATIO);
            assertThat(sizes).isNotNull();

            final Camera.Size usedSize = parameters.getPictureSize();
            assertThat(usedSize.width).isEqualTo(sizes.first.width);
            assertThat(usedSize.height).isEqualTo(sizes.first.height);
            return null;
        });
    }

    private void openCameraWithCameraController(final Function<Camera, Void> callback) {
        try (final ActivityScenario<NoOpActivity> scenario = ActivityScenario.launch(NoOpActivity.class)) {
            final AtomicReference<CompletableFuture<Void>> reference = new AtomicReference<>();
            scenario.onActivity(activity -> {
                mCameraController = new CameraController(activity);
                activity.getCameraPreviewContainer().addView(mCameraController.getPreviewView(getTargetContext()));
                reference.set(mCameraController.open());
            });

            CompletableFuture<Void> openCameraFuture;
            do {
                openCameraFuture = reference.get();
            } while (openCameraFuture == null);

            openCameraFuture.join();

            callback.apply(mCameraController.getCamera());
        }
    }

    @Test
    @RequiresDevice
    public void should_useLargestPreviewResolution_withSimilarAspectRatio_asPictureSize() {
        openCameraWithCameraController(camera -> {
            final Camera.Parameters parameters = camera.getParameters();
            final Size pictureSize = new Size(parameters.getPictureSize().width,
                    parameters.getPictureSize().height);
            final Size largestSize = SizeSelectionHelper.getLargestAllowedSizeWithSimilarAspectRatio(
                    parameters.getSupportedPreviewSizes()
                            .stream()
                            .map(size -> new Size(size.width, size.height))
                            .collect(Collectors.toList()),
                    pictureSize, CameraResolutionRequirement.MAX_PICTURE_AREA);
            assertThat(largestSize).isNotNull();
            final Camera.Size usedSize = parameters.getPreviewSize();
            assertThat(usedSize.width).isEqualTo(largestSize.width);
            assertThat(usedSize.height).isEqualTo(largestSize.height);
            return null;
        });
    }

    @Test
    @Ignore("TODO: implement w/o using a camera spy")
    public void should_useContinuousFocusMode_ifAvailable() {
        // TODO: implement w/o using a camera spy
    }

    @Test
    @Ignore("TODO: implement w/o using a camera spy")
    public void should_useAutoFocusMode_ifContinuousFocusMode_isNotAvailable() {
        // TODO: implement w/o using a camera spy
    }

    @Test
    @Ignore("TODO: implement w/o using a camera spy")
    public void should_doAutoFocusRun_beforeTakingPicture_ifNoContinuousFocusMode() {
        // TODO: implement w/o using a camera spy
    }

    @Test
    @Ignore("TODO: implement w/o using a camera spy")
    public void should_notDoAutoFocusRun_beforeTakingPicture_ifUsingContinuousFocusMode() {
        // TODO: implement w/o using a camera spy
    }
}