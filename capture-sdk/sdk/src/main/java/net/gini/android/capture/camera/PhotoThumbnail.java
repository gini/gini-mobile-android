package net.gini.android.capture.camera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import net.gini.android.capture.R;

/**
 * Created by Alpar Szotyori on 13.02.2018.
 *
 * Copyright (c) 2018 Gini GmbH.
 */

public class PhotoThumbnail extends ConstraintLayout {

    private TextView badge;
    private OnClickListener clickListener;
    private ImageView thumbnail;

    public PhotoThumbnail(final Context context) {
        super(context, null, 0);
        init(context);
    }

    private void init(@NonNull final Context context) {
        final LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.gc_photo_thumbnail, this);

        thumbnail = findViewById(R.id.gc_thumbnail);
        badge = findViewById(R.id.gc_badge);
        if (!isInEditMode()) {
            badge.setVisibility(INVISIBLE);
        }
    }

    public PhotoThumbnail(final Context context,
                          @Nullable final AttributeSet attrs) {
        super(context, attrs, 0);
        init(context);
    }

    public PhotoThumbnail(final Context context, @Nullable final AttributeSet attrs,
                          final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public PhotoThumbnail(final Context context, @Nullable final AttributeSet attrs,
                          final int defStyleAttr,
                          final int defStyleRes) { // NOPMD
        super(context, attrs, defStyleAttr);
        init(context);
    }

    @Override
    public void setOnClickListener(@Nullable final OnClickListener clickListener) {
        this.clickListener = clickListener;
    }

    private void addClickListener(@NonNull final View view) {
        final boolean clickable = clickListener != null;
        view.setClickable(clickable);
        view.setFocusable(clickable);
        view.setOnClickListener(clickListener);
    }

    public void setImage(@Nullable final ThumbnailBitmap thumbnailBitmap) {
        setBitmapOrBlack(thumbnail, thumbnailBitmap);
        addClickListener(thumbnail);
    }

    public void setImageCount(final int count) {
        badge.setText(String.valueOf(count));
        badge.setVisibility(count > 0 ? VISIBLE : INVISIBLE);
    }

    public void removeImage() {
        removeClickListener(thumbnail);
        resetImageView(thumbnail);
        setImageCount(0);
    }

    private void removeClickListener(
            @NonNull final View view) {
        view.setOnClickListener(null);
        view.setClickable(false);
        view.setFocusable(false);
    }

    private void resetImageView(@NonNull final ImageView imageView) {
        imageView.setImageDrawable(null);
        imageView.setBackgroundColor(Color.TRANSPARENT);
    }

    private static void setBitmapOrBlack(@NonNull final ImageView imageView,
            @Nullable final ThumbnailBitmap stackBitmap) {
        if (stackBitmap != null) {
            imageView.setImageBitmap(stackBitmap.getRotatedBitmap());
        } else {
            imageView.setImageBitmap(null);
            imageView.setBackgroundColor(Color.BLACK);
        }
    }

    static class ThumbnailBitmap {

        Bitmap bitmap;
        private Bitmap rotatedBitmap;
        int rotation;

        ThumbnailBitmap(final Bitmap bitmap, final int rotation) {
            this.bitmap = bitmap;
            this.rotation = rotation;
        }

        final Bitmap getRotatedBitmap() {
            if (rotatedBitmap == null) {
                Matrix matrix = new Matrix();
                matrix.postRotate(rotation);
                rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            }
            return rotatedBitmap;
        }
    }

}
