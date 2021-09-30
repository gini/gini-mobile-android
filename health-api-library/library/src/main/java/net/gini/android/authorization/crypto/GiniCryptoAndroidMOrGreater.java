package net.gini.android.authorization.crypto;

import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Created by Alpar Szotyori on 08.10.2018.
 *
 * Copyright (c) 2018 Gini GmbH.
 */
@RequiresApi(api = Build.VERSION_CODES.M)
class GiniCryptoAndroidMOrGreater extends GiniCrypto {

    GiniCryptoAndroidMOrGreater() {
    }

    Cipher createCipher(int cipherOpMode, @NonNull byte[] iv)
            throws GiniCryptoException {
        try {
            final Cipher cipher = Cipher.getInstance(AES_MODE);
            cipher.init(cipherOpMode, getSecretKey(), new GCMParameterSpec(128, iv));
            return cipher;
        } catch (NoSuchPaddingException | InvalidAlgorithmParameterException | InvalidKeyException
                | NoSuchAlgorithmException e) {
            throw new GiniCryptoException(e);
        }
    }

    @Override
    Key getSecretKey() throws GiniCryptoException {
        try {
            if (!hasSecretKey()) {
                generateSecretKey();
            }
            return getKeyStore().getKey(SECRET_KEY_ALIAS, null);
        } catch (IOException | CertificateException | UnrecoverableKeyException
                | NoSuchAlgorithmException | NoSuchProviderException
                | InvalidAlgorithmParameterException | KeyStoreException e) {
            throw new GiniCryptoException(e);
        }
    }

    private boolean hasSecretKey()
            throws CertificateException, NoSuchAlgorithmException, IOException, KeyStoreException {
        return getKeyStore().containsAlias(SECRET_KEY_ALIAS);
    }

    @Override
    KeyStore getKeyStore()
            throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException {
        final KeyStore keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
        keyStore.load(null);
        return keyStore;
    }

    private void generateSecretKey() throws NoSuchProviderException, NoSuchAlgorithmException,
            InvalidAlgorithmParameterException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEY_STORE);
        final KeyGenParameterSpec spec = new KeyGenParameterSpec
                .Builder(SECRET_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(false)
                .build();
        keyGenerator.init(spec);
        keyGenerator.generateKey();
    }
}
