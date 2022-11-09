package net.gini.android.capture.analysis;


import net.gini.android.capture.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

public enum AnalysisHint {
    FLAT(R.drawable.gc_photo_tip_flat, R.string.gc_photo_tip_flat, R.string.gc_photo_tip_flatten_the_page_title),
    ALIGN(R.drawable.gc_photo_tip_align, R.string.gc_photo_tip_align, R.string.gc_photo_tip_align_title),
    PARALLEL(R.drawable.gc_photo_tip_parallel, R.string.gc_photo_tip_parallel, R.string.gc_photo_tip_parallel_title),
    LIGHTING(R.drawable.gc_photo_tip_lighting, R.string.gc_photo_tip_lighting, R.string.gc_photo_tip_good_lightning_title),
    MULTIPAGE(R.drawable.gc_photo_tip_multipage, R.string.gc_photo_tip_multipage, R.string.gc_photo_tip_multiple_pages_title);

    public int getDrawableResource() {
        return mDrawableResource;
    }

    public int getTextResource() {
        return mTextResource;
    }

    public int getTitleTextResource() {
        return mTitleTextResource;
    }

    private final int mDrawableResource;
    private final int mTextResource;
    private final int mTitleTextResource;

    AnalysisHint(@DrawableRes final int drawableResource, @StringRes final int textResource, @StringRes final int titleTextResource) {
        mDrawableResource = drawableResource;
        mTextResource = textResource;
        mTitleTextResource = titleTextResource;
    }

    static List<AnalysisHint> getArray() {
        final List<AnalysisHint> arrayList = new ArrayList<>(values().length);
        Collections.addAll(arrayList, values());
        return arrayList;
    }
}
