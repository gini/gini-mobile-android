package net.gini.android.capture.internal.camera.api;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.hardware.Camera;

import net.gini.android.capture.internal.util.Size;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(JUnit4.class)
public class CameraParametersHelperTest {

    private Camera mCamera;
    private Camera.Parameters mParameters;

    @Before
    public void setUp() throws Exception {
        // Mock camera and parameters
        mCamera = mock(Camera.class);
        mParameters = mock(Camera.Parameters.class);
        when(mCamera.getParameters()).thenReturn(mParameters);
    }

    @Test
    public void should_verifyThatFocusMode_isSupported() {
        // Mock supported focus modes
        when(mParameters.getSupportedFocusModes()).thenReturn(
                Arrays.asList(Camera.Parameters.FOCUS_MODE_AUTO,
                        Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE));

        boolean isSupported = CameraParametersHelper.isFocusModeSupported(
                Camera.Parameters.FOCUS_MODE_AUTO, mCamera);

        assertThat(isSupported).isTrue();
    }

    @Test
    public void should_verifyThatFocusMode_isNotSupported() {
        // Mock supported focus modes
        when(mParameters.getSupportedFocusModes()).thenReturn(
                Collections.singletonList(Camera.Parameters.FOCUS_MODE_AUTO));

        boolean isSupported = CameraParametersHelper.isFocusModeSupported(
                Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE, mCamera);

        assertThat(isSupported).isFalse();
    }

    @Test
    public void should_verifyThatFocusMode_isUsed() {
        // Mock used focus mode
        when(mParameters.getFocusMode()).thenReturn(Camera.Parameters.FOCUS_MODE_AUTO);

        boolean isUsed = CameraParametersHelper.isUsingFocusMode(
                Camera.Parameters.FOCUS_MODE_AUTO, mCamera);

        assertThat(isUsed).isTrue();
    }

    @Test
    public void should_verifyThatFocusMode_isNotUsed() {
        // Mock used focus mode
        when(mParameters.getFocusMode()).thenReturn(Camera.Parameters.FOCUS_MODE_AUTO);

        boolean isUsed = CameraParametersHelper.isUsingFocusMode(
                Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE, mCamera);

        assertThat(isUsed).isFalse();
    }

    @Test
    public void should_verifyThatFlashMode_isSupported() {
        // Mock used flash mode
        when(mParameters.getSupportedFlashModes())
                .thenReturn(Arrays.asList(Camera.Parameters.FLASH_MODE_AUTO,
                        Camera.Parameters.FLASH_MODE_TORCH));

        boolean isSupported = CameraParametersHelper.isFlashModeSupported(
                Camera.Parameters.FLASH_MODE_AUTO, mCamera);

        assertThat(isSupported).isTrue();
    }

    @Test
    public void should_verifyThatFlashMode_isNotSupported() {
        // Mock used flash mode
        when(mParameters.getSupportedFlashModes())
                .thenReturn(Collections.singletonList(Camera.Parameters.FLASH_MODE_AUTO));

        boolean isSupported = CameraParametersHelper.isFlashModeSupported(
                Camera.Parameters.FLASH_MODE_TORCH, mCamera);

        assertThat(isSupported).isFalse();
    }

    @Test
    public void should_getSupportedPictureSizes_asListOfSizes() {
        // Given
        // Returning an empty list because Camera.Size cannot be instantiated outside of an android.hardware.Camera
        // instance.
        when(mParameters.getSupportedPictureSizes()).thenReturn(Collections.emptyList());

        // When
        final List<Size> supportedPictureSizes = CameraParametersHelper.getSupportedPictureSizes(mParameters);

        // Then
        verify(mParameters).getSupportedPictureSizes();
        assertThat(supportedPictureSizes).isEmpty();
    }

    @Test
    public void should_getSupportedPreviewSizes_asListOfSizes() {
        // Given
        // Returning an empty list because Camera.Size cannot be instantiated outside of an android.hardware.Camera
        // instance.
        when(mParameters.getSupportedPreviewSizes()).thenReturn(Collections.emptyList());

        // When
        final List<Size> supportedPictureSizes = CameraParametersHelper.getSupportedPreviewSizes(mParameters);

        // Then
        verify(mParameters).getSupportedPreviewSizes();
        assertThat(supportedPictureSizes).isEmpty();
    }

}