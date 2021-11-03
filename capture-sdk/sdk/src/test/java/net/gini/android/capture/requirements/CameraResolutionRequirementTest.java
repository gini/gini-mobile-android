package net.gini.android.capture.requirements;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import net.gini.android.capture.internal.util.Size;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(JUnit4.class)
public class CameraResolutionRequirementTest {

    @Test
    public void should_reportUnfulfilled_ifNoPreviewSize_withSameAspectRatio_asLargestPictureSize() {
        OldCameraApiHolder cameraHolder = getCameraHolder(Collections.singletonList(new Size(300, 200)),
                null);

        CameraResolutionRequirement requirement = new CameraResolutionRequirement(cameraHolder);

        assertThat(requirement.check().isFulfilled()).isFalse();
        assertThat(requirement.check().getDetails()).isEqualTo(
                "Camera doesn't have a resolution that matches the requirements");
    }

    @Test
    public void should_reportUnfulfilled_ifPictureSize_isSmallerThan8MP() {
        OldCameraApiHolder cameraHolder = getCameraHolder(null,
                Arrays.asList(
                        new Size(400, 300),
                        new Size(3200, 2048)) //6,55MP
        );

        CameraResolutionRequirement requirement = new CameraResolutionRequirement(cameraHolder);

        assertThat(requirement.check().isFulfilled()).isFalse();
        assertThat(requirement.check().getDetails()).isEqualTo(
                "Camera doesn't have a resolution that matches the requirements");
    }

    @Test
    public void should_reportFulfilled_ifPreviewSize_andPictureSize_isLargerThan8MP() {
        OldCameraApiHolder cameraHolder = getCameraHolder(null, null);

        CameraResolutionRequirement requirement = new CameraResolutionRequirement(cameraHolder);

        assertThat(requirement.check().isFulfilled()).isTrue();
        assertThat(requirement.check().getDetails()).isEqualTo("");
    }

    @Test
    public void should_reportUnfulfilled_ifCamera_isNotOpen() {
        OldCameraApiHolder cameraHolder = new OldCameraApiHolder();

        CameraResolutionRequirement requirement = new CameraResolutionRequirement(cameraHolder);

        assertThat(requirement.check().isFulfilled()).isFalse();
        assertThat(requirement.check().getDetails()).isEqualTo("Camera not open");
    }

    private OldCameraApiHolder getCameraHolder(List<Size> previewSizes,
                                               List<Size> pictureSizes) {
        OldCameraApiHolder cameraHolder = mock(OldCameraApiHolder.class);
        if (previewSizes == null) {
            Size size4to3 = new Size(1440, 1080);
            Size size16to9 = new Size(1280, 720);
            previewSizes = Arrays.asList(size4to3, size16to9);
        }
        if (pictureSizes == null) {
            Size size4to3 = new Size(2880, 2160);
            Size size16to9 = new Size(3840, 2160);
            pictureSizes = Arrays.asList(size4to3, size16to9);
        }
        when(cameraHolder.getSupportedPreviewSizes()).thenReturn(previewSizes);
        when(cameraHolder.getSupportedPictureSizes()).thenReturn(pictureSizes);
        return cameraHolder;
    }
}
