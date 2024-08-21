package net.gini.android.capture.internal.fileimport.providerchooser;

import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;

/**
 * Internal use only.
 *
 * @suppress
 */
public class ProvidersAppWrapperItem extends ProvidersItem {

    private final Drawable mIcon;
    private final String mText;

    public ProvidersAppWrapperItem(@NonNull final Drawable icon, @NonNull final String text) {
        super(FileProviderItemType.APP_WRAPPER_PHOTO_PICKER);
        mIcon = icon;
        mText = text;
    }

    public Drawable getDrawableIcon() {
        return mIcon;
    }

    public String getText() {
        return mText;
    }

}
