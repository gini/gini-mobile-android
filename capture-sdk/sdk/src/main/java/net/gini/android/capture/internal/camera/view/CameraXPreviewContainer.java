package net.gini.android.capture.internal.camera.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.camera.view.PreviewView;

import android.util.Size;

/**
 * Internal use only.
 *
 * @suppress
 */
public class CameraXPreviewContainer extends FrameLayout {

    private Size mPreviewSize;

    /**
     * Internal use only.
     *
     * @suppress
     */
    public enum ScaleType {
        CENTER_RESIZE,
        CENTER_INSIDE
    }

    private ScaleType mScaleType = ScaleType.CENTER_INSIDE;

    private final PreviewView mPreviewView;

    public CameraXPreviewContainer(final Context context) {
        super(context);
        mPreviewView = createPreviewView(context);
        init();
    }

    public CameraXPreviewContainer(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        mPreviewView = createPreviewView(context);
        init();
    }

    public CameraXPreviewContainer(final Context context, final AttributeSet attrs,
                                   final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mPreviewView = createPreviewView(context);
        init();
    }

    private PreviewView createPreviewView(final Context context) {
        final PreviewView previewView = new PreviewView(context);
        previewView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        return previewView;
    }

    private void init() {
        addView(mPreviewView);
    }

    public PreviewView getPreviewView() {
        return mPreviewView;
    }

    public void setPreviewSize(final Size previewSize) {
        mPreviewSize = previewSize;
        requestLayout();
    }

    public void setScaleType(final ScaleType scaleType) {
        mScaleType = scaleType;
        requestLayout();
    }

    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        final int width = getWidth();
        final int height = getHeight();

        if (width > 0 && height > 0
                && mPreviewSize != null) {
            final float aspectRatioSurface = (float) width / (float) height;
            final float aspectRatioPreview =
                    (float) mPreviewSize.getWidth() / (float) mPreviewSize.getHeight();

            int adjustedWidth = width;
            int adjustedHeight = height;

            switch (mScaleType) {
                case CENTER_RESIZE:
                    if (aspectRatioSurface < aspectRatioPreview) {
                        // surface width < preview width AND surface height > preview height
                        // Keep the height and change the width to resize the surface to the preview's aspect ratio
                        adjustedWidth = (int) (height * aspectRatioPreview);
                    } else if (aspectRatioSurface > aspectRatioPreview) {
                        // surface width > preview width AND surface height < preview height
                        // Keep the width and change the height to resize the surface to the preview's aspect ratio
                        adjustedHeight = (int) (width / aspectRatioPreview);
                    }
                    break;
                case CENTER_INSIDE:
                    if (aspectRatioSurface < aspectRatioPreview) {
                        // surface width < preview width AND surface height > preview height
                        // Keep the width and change the height to fit the preview inside the surface's original size
                        adjustedHeight = (int) (width / aspectRatioPreview);
                    } else if (aspectRatioSurface > aspectRatioPreview) {
                        // surface width > preview width AND surface height < preview height
                        // Keep the height and change the width to fit the preview inside the surface's original size
                        adjustedWidth = (int) (height * aspectRatioPreview);
                    }
                    break;
                default:
                    break;
            }

            measureChild(getChildAt(0), MeasureSpec.makeMeasureSpec(adjustedWidth, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(adjustedHeight, MeasureSpec.EXACTLY));
            setMeasuredDimension(adjustedWidth, adjustedHeight);
        }
    }
}
