package net.gini.android.capture.internal.camera.api;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.hardware.Camera;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.gini.android.capture.document.ImageDocument;
import net.gini.android.capture.internal.camera.photo.Photo;
import net.gini.android.capture.internal.camera.photo.PhotoFactory;
import net.gini.android.capture.internal.util.Size;

import org.jetbrains.annotations.NotNull;

import jersey.repackaged.jsr166e.CompletableFuture;

/**
 * Created by Alpar Szotyori on 15.12.2017.
 *
 * Copyright (c) 2017 Gini GmbH.
 */

public class CameraControllerFake implements CameraInterface {

    private static final Size DEFAULT_PREVIEW_SIZE = new Size(900, 1200);
    private Photo mPhoto;
    private PreviewCallback mPreviewCallback;
    private Size mPreviewSize = DEFAULT_PREVIEW_SIZE;
    private boolean mFlashEnabled;

    @NonNull
    @Override
    public CompletableFuture<Void> open() {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void close() {

    }

    @NonNull
    @Override
    public CompletableFuture<Void> startPreview() {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void stopPreview() {

    }

    @Override
    public boolean isPreviewRunning() {
        return true;
    }

    @Override
    public void enableTapToFocus(@Nullable final TapToFocusListener listener) {

    }

    @Override
    public void disableTapToFocus() {

    }

    @NonNull
    @Override
    public CompletableFuture<Boolean> focus() {
        return CompletableFuture.completedFuture(true);
    }

    @NonNull
    @Override
    public CompletableFuture<Photo> takePicture() {
        return CompletableFuture.completedFuture(mPhoto);
    }

    @Override
    public void setPreviewCallback(@Nullable PreviewCallback previewCallback) {
        mPreviewCallback = previewCallback;
    }

    public PreviewCallback getPreviewCallback() {
        return mPreviewCallback;
    }

    @Override
    public View getPreviewView(@NonNull @NotNull Context context) {
        final View view = new View(context);
        final ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        view.setLayoutParams(layoutParams);
        return view;
    }

    @Override
    public boolean isFlashAvailable() {
        return true;
    }

    @Override
    public boolean isFlashEnabled() {
        return mFlashEnabled;
    }

    @Override
    public void setFlashEnabled(final boolean enabled) {
        mFlashEnabled = enabled;
    }

    public void showImageAsPreview(@NonNull final byte[] image, @Nullable final byte[] imageNV21) {
        mPhoto = PhotoFactory.newPhotoFromJpeg(image, 0, "portrait", "photo",
                ImageDocument.Source.newCameraSource());

        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(image, 0, image.length, options);
        mPreviewSize = new Size(options.outWidth, options.outHeight);

        if (mPreviewCallback != null && imageNV21 != null) {
            mPreviewCallback.onPreviewFrame(imageNV21, mPreviewSize, 0);
        }
    }
}
