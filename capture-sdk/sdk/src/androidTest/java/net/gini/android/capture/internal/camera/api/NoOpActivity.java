package net.gini.android.capture.internal.camera.api;

import android.app.Activity;
import android.os.Bundle;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import net.gini.android.capture.internal.camera.view.CameraPreviewContainer;
import net.gini.android.capture.test.R;

public class NoOpActivity extends Activity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_no_op);
    }

    public CameraPreviewContainer getCameraPreviewContainer() {
        return findViewById(R.id.camera_preview_container);
    }
}
