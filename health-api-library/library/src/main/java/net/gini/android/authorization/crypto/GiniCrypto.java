package net.gini.android.authorization.crypto;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import android.util.Base64;

import java.io.IOException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;

/**
 * Created by Alpar Szotyori on 08.10.2018.
 *
 * Copyright (c) 2018 Gini GmbH.
 */
public abstract class GiniCrypto {

    static final String ANDROID_KEY_STORE = "AndroidKeyStore";
    static final String SECRET_KEY_ALIAS = "GiniCryptoKey";
    static final String AES_MODE = "AES/GCM/NoPadding";

    public static GiniCrypto newInstance(@NonNull final SharedPreferences sharedPreferences,
            @NonNull final Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return new GiniCryptoAndroidMOrGreater();
        } else {
            return new GiniCryptoPreAndroidM(sharedPreferences, context);
        }
    }

    public String encrypt(@NonNull final String text) throws GiniCryptoException {
        try {
            final byte[] iv = generateIV();
            final Cipher cipher = createCipher(Cipher.ENCRYPT_MODE, iv);
            final byte[] encodedBytes = cipher.doFinal(text.getBytes());
            final byte[] encodedBytesWithIV = prependIV(iv, encodedBytes);
            return Base64.encodeToString(encodedBytesWithIV, Base64.DEFAULT);
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            throw new GiniCryptoException(e);
        }
    }

    abstract Cipher createCipher(int cipherOpMode, @NonNull byte[] iv) throws GiniCryptoException;

    private byte[] prependIV(final byte[] iv, final byte[] encodedBytes) {
        final byte[] encodedBytesWithIV = new byte[encodedBytes.length + iv.length + 1];
        encodedBytesWithIV[0] = (byte) iv.length;
        System.arraycopy(iv, 0, encodedBytesWithIV, 1, iv.length);
        System.arraycopy(encodedBytes, 0, encodedBytesWithIV, iv.length + 1,
                encodedBytes.length);
        return encodedBytesWithIV;
    }

    private byte[] generateIV() {
        final SecureRandom secureRandom = new SecureRandom();
        final byte[] iv = new byte[12];
        secureRandom.nextBytes(iv);
        return iv;
    }

    public String decrypt(@NonNull final String encrypted) throws GiniCryptoException {
        try {
            final byte[] encryptedBytes = Base64.decode(encrypted, Base64.DEFAULT);
            final byte[] iv = readIV(encryptedBytes);
            final Cipher cipher = createCipher(Cipher.DECRYPT_MODE, iv);
            final int inputOffset = iv.length + 1;
            final byte[] decryptedBytes = cipher.doFinal(encryptedBytes, inputOffset,
                    encryptedBytes.length - inputOffset);
            return new String(decryptedBytes);
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            throw new GiniCryptoException(e);
        }
    }

    private byte[] readIV(final byte[] encryptedBytes) {
        final byte[] iv = new byte[encryptedBytes[0]];
        System.arraycopy(encryptedBytes, 1, iv, 0, iv.length);
        return iv;
    }

    abstract Key getSecretKey() throws GiniCryptoException;

    @VisibleForTesting
    void deleteSecretKey()
            throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException {
        getKeyStore().deleteEntry(SECRET_KEY_ALIAS);
    }

    abstract KeyStore getKeyStore()
            throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException;

}
