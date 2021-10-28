package net.gini.android.capture.requirements;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.hardware.Camera;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Collections;

import kotlin.Pair;

@RunWith(JUnit4.class)
public class CameraFocusRequirementTest {

    @Test
    public void should_reportUnfulfilled_ifAutoFocus_isNotSupported() {
        OldCameraApiHolder cameraHolder = getCameraHolder(false);

        CameraFocusRequirement requirement = new CameraFocusRequirement(cameraHolder);

        assertThat(requirement.check().isFulfilled()).isFalse();
    }

    @Test
    public void should_reportFulfilled_ifAutoFocus_isSupported() {
        OldCameraApiHolder cameraHolder = getCameraHolder(true);

        CameraFocusRequirement requirement = new CameraFocusRequirement(cameraHolder);

        assertThat(requirement.check().isFulfilled()).isTrue();
    }

    @Test
    public void should_reportUnfulfilled_ifCamera_isNotOpen() {
        OldCameraApiHolder cameraHolder = new OldCameraApiHolder();

        CameraFocusRequirement requirement = new CameraFocusRequirement(cameraHolder);

        assertThat(requirement.check().isFulfilled()).isFalse();
    }

    public OldCameraApiHolder getCameraHolder(boolean isAutoFocusSupported) {
        OldCameraApiHolder cameraHolder = mock(OldCameraApiHolder.class);
        when(cameraHolder.hasAutoFocus()).thenReturn(new Pair<>(isAutoFocusSupported, ""));
        return cameraHolder;
    }
}
