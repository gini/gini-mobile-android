package net.gini.android.capture.test;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;

import android.app.Instrumentation;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import androidx.annotation.NonNull;
import androidx.test.platform.app.InstrumentationRegistry;

import net.gini.android.capture.internal.util.ContextHelper;

/**
 * Created by Alpar Szotyori on 15.05.2019.
 *
 * Copyright (c) 2019 Gini GmbH.
 */
public final class Helpers {

    private Helpers() {
    }

    public static byte[] getTestJpeg() throws IOException {
        return loadAsset("invoice.jpg");
    }

    public static byte[] loadAsset(final String filename) throws IOException {
        final AssetManager assetManager = getApplicationContext().getAssets();
        InputStream inputStream = null;
        try {
            inputStream = assetManager.open(filename);
            return inputStreamToByteArray(inputStream);
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }

    private static byte[] inputStreamToByteArray(final InputStream inputStream) throws IOException {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] bytes;
        try {
            final byte[] buffer = new byte[8192];
            int readBytes;
            while ((readBytes = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, readBytes);
            }
            bytes = outputStream.toByteArray();
        } finally {
            outputStream.close();
        }
        return bytes;
    }

    public static void copyAssetToStorage(@NonNull final String assetFilePath,
            @NonNull final String storageDirPath) throws IOException {
        final AssetManager assetManager = getApplicationContext().getAssets();
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            inputStream = assetManager.open(assetFilePath);
            final File file = new File(storageDirPath,
                    Uri.parse(assetFilePath).getLastPathSegment());
            if (file.exists() || file.createNewFile()) {
                outputStream = new FileOutputStream(file);
                copyFile(inputStream, outputStream);
            } else {
                throw new IOException("Could not create file: " + file.getAbsolutePath());
            }
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
            if (outputStream != null) {
                outputStream.close();
            }
        }
    }

    private static void copyFile(final InputStream inputStream, final OutputStream outputStream)
            throws IOException {
        final byte[] buffer = new byte[8192];
        int read;
        while ((read = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, read);
        }
    }

    public static <T extends Parcelable, C extends Parcelable.Creator<T>> T doParcelingRoundTrip(
            final T payload, final C creator) {
        final Parcel parcel = Parcel.obtain();
        payload.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        return creator.createFromParcel(parcel);
    }

    public static boolean isTablet() {
        final Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        return ContextHelper.isTablet(instrumentation.getTargetContext());
    }
}
