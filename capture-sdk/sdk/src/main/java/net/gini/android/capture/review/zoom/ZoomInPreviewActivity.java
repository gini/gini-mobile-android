package net.gini.android.capture.review.zoom;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import net.gini.android.capture.GiniCapture;
import net.gini.android.capture.R;
import net.gini.android.capture.document.ImageDocument;
import net.gini.android.capture.review.multipage.MultiPageReviewFragment;
import net.gini.android.capture.view.InjectedViewContainer;
import net.gini.android.capture.view.NavButtonType;
import net.gini.android.capture.view.NavigationBarTopAdapter;

/**
 * Internal use only.
 */
public class ZoomInPreviewActivity extends AppCompatActivity {

    private static final String ARGS_DOCUMENT = "GC_ARGS_DOCUMENT";

    private ZoomInPreviewFragment mZoomInFragment;
    private ImageDocument mImageDocument;

    private ImageButton mClose;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gc_activity_zoom_in_preview);

        initViews();

        mImageDocument = getIntent().getParcelableExtra(ARGS_DOCUMENT);

        addFragment();
    }

    private void initViews() {
        mClose = findViewById(R.id.gc_action_close);

        mClose.setOnClickListener(v -> onBackPressed());
    }


    private void addFragment() {
        mZoomInFragment = ZoomInPreviewFragment.newInstance(mImageDocument);
        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.gc_fragment_container, mZoomInFragment)
                .commit();
    }
}
