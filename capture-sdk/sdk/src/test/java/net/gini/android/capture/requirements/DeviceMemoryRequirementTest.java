package net.gini.android.capture.requirements;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.hardware.Camera;

import net.gini.android.capture.internal.util.Size;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(JUnit4.class)
public class DeviceMemoryRequirementTest {

    @Test
    public void should_reportUnfulfilled_ifCamera_isNotOpen() {
        OldCameraApiHolder cameraHolder = mock(OldCameraApiHolder.class);

        DeviceMemoryRequirement requirement = new DeviceMemoryRequirement(cameraHolder);

        assertThat(requirement.check().isFulfilled()).isFalse();
    }

    @Test
    public void should_reportUnfulfilled_ifEnoughMemory_isNotAvailable() {
        OldCameraApiHolder cameraHolder = getCameraHolder(null);

        DeviceMemoryRequirement requirement = spy(new DeviceMemoryRequirement(cameraHolder));

        doReturn(false).when(requirement).sufficientMemoryAvailable(any(Size.class));

        assertThat(requirement.check().isFulfilled()).isFalse();
    }

    @Test
    public void should_reportFulfilled_ifEnoughMemory_isAvailable() {
        OldCameraApiHolder cameraHolder = getCameraHolder(
                Collections.singletonList(new Size(3264, 2448)));

        DeviceMemoryRequirement requirement = new DeviceMemoryRequirement(cameraHolder);

        assertThat(requirement.check().isFulfilled()).isTrue();
    }

    @Test
    public void should_checkIfPictureSize_fitsIntoUnusedMemory() {
        OldCameraApiHolder cameraHolder = getCameraHolder(null);
        DeviceMemoryRequirement requirement = new DeviceMemoryRequirement(cameraHolder);

        // Unused memory = max - (total - free)
        final long totalMemoryByte = 28 * 1024 * 1024;
        final long freeMemoryByte = 1024 * 1024;
        final long maxMemoryByte = 32 * 1024 * 1024;

        // Required memory: size.width * size.height * 3 * 3
        assertThat(requirement.sufficientMemoryAvailable(totalMemoryByte, freeMemoryByte, maxMemoryByte,
                new Size(800, 600))).isTrue();
        assertThat(requirement.sufficientMemoryAvailable(totalMemoryByte, freeMemoryByte, maxMemoryByte,
                new Size(1024, 768))).isFalse();
    }

    private OldCameraApiHolder getCameraHolder(List<Size> pictureSizes) {
        OldCameraApiHolder cameraHolder = mock(OldCameraApiHolder.class);
        if (pictureSizes == null) {
            Size size4to3 = new Size(4128, 3096);
            Size sizeOther = new Size(4128, 2322);
            pictureSizes = Arrays.asList(size4to3, sizeOther);
        }
        when(cameraHolder.getSupportedPictureSizes()).thenReturn(pictureSizes);
        when(cameraHolder.getSupportedPreviewSizes()).thenReturn(pictureSizes);
        return cameraHolder;
    }

}
