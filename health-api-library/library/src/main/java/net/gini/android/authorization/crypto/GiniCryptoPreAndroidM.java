package net.gini.android.authorization.crypto;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.KeyPairGeneratorSpec;
import androidx.annotation.NonNull;
import android.text.TextUtils;
import android.util.Base64;

import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.util.Calendar;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.x500.X500Principal;

/**
 * Created by Alpar Szotyori on 08.10.2018.
 *
 * Copyright (c) 2018 Gini GmbH.
 */
class GiniCryptoPreAndroidM extends GiniCrypto {

    private static final String AES_KEY = "GiniKey";
    private static final String RSA_MODE = "RSA/ECB/PKCS1Padding";

    private final SharedPreferences mSharedPreferences;
    private final Context mContext;

    GiniCryptoPreAndroidM(@NonNull final SharedPreferences sharedPreferences,
            @NonNull final Context context) {
        mSharedPreferences = sharedPreferences;
        mContext = context;
    }

    Cipher createCipher(int cipherOpMode, @NonNull byte[] iv)
            throws GiniCryptoException {
        try {
            final Cipher cipher = Cipher.getInstance(AES_MODE);
            cipher.init(cipherOpMode, getSecretKey(), new IvParameterSpec(iv));
            return cipher;
        } catch (NoSuchPaddingException | InvalidAlgorithmParameterException | InvalidKeyException
                | NoSuchAlgorithmException e) {
            throw new GiniCryptoException(e);
        }
    }

    @Override
    Key getSecretKey() throws GiniCryptoException {
        try {
            return new SecretKeySpec(getAESKey(), "AES");
        } catch (InvalidKeyException | UnrecoverableEntryException | NoSuchAlgorithmException
                | KeyStoreException | NoSuchProviderException | InvalidAlgorithmParameterException
                | CertificateException | IllegalBlockSizeException | BadPaddingException
                | IOException | NoSuchPaddingException | IllegalArgumentException e) {
            throw new GiniCryptoException(e);
        }
    }

    @Override
    KeyStore getKeyStore()
            throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException {
        final KeyStore keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
        keyStore.load(null);
        return keyStore;
    }

    private byte[] getAESKey() throws IOException, CertificateException, NoSuchAlgorithmException,
            NoSuchProviderException, InvalidKeyException, NoSuchPaddingException,
            BadPaddingException, UnrecoverableEntryException,
            KeyStoreException, IllegalBlockSizeException, InvalidAlgorithmParameterException {
        final String encryptedBase64 = mSharedPreferences.getString(AES_KEY, "");
        if (TextUtils.isEmpty(encryptedBase64)) {
            final byte[] aesKey = generateAESKey();
            saveAESKey(aesKey);
            return aesKey;
        } else {
            final byte[] encrypted = Base64.decode(encryptedBase64, Base64.DEFAULT);
            return rsaDecrypt(encrypted);
        }
    }

    private byte[] generateAESKey() throws NoSuchAlgorithmException {
        final KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(256);
        return keyGenerator.generateKey().getEncoded();
    }

    private void saveAESKey(@NonNull final byte[] aesKey)
            throws CertificateException, NoSuchAlgorithmException,
            InvalidKeyException, UnrecoverableEntryException, NoSuchPaddingException,
            BadPaddingException, IOException, KeyStoreException, NoSuchProviderException,
            IllegalBlockSizeException, InvalidAlgorithmParameterException {
        final byte[] encrypted = rsaEncrypt(aesKey);
        final String encryptedBase64 = Base64.encodeToString(encrypted, Base64.DEFAULT);
        mSharedPreferences.edit()
                .putString(AES_KEY, encryptedBase64)
                .apply();
    }

    private byte[] rsaEncrypt(byte[] secret)
            throws NoSuchPaddingException, NoSuchAlgorithmException, NoSuchProviderException,
            InvalidKeyException, BadPaddingException, IllegalBlockSizeException,
            CertificateException, UnrecoverableEntryException,
            KeyStoreException, IOException, InvalidAlgorithmParameterException {
        final KeyStore.PrivateKeyEntry privateKeyEntry = getKeyPair();
        final Cipher cipher = Cipher.getInstance(RSA_MODE, "AndroidOpenSSL");
        cipher.init(Cipher.ENCRYPT_MODE, privateKeyEntry.getCertificate().getPublicKey());
        return cipher.doFinal(secret);
    }

    private byte[] rsaDecrypt(byte[] encrypted)
            throws CertificateException, NoSuchAlgorithmException, KeyStoreException,
            UnrecoverableEntryException, IOException, NoSuchProviderException,
            NoSuchPaddingException, InvalidKeyException, BadPaddingException,
            IllegalBlockSizeException, InvalidAlgorithmParameterException {
        final KeyStore.PrivateKeyEntry privateKeyEntry = getKeyPair();
        final Cipher cipher = Cipher.getInstance(RSA_MODE, "AndroidOpenSSL");
        cipher.init(Cipher.DECRYPT_MODE, privateKeyEntry.getPrivateKey());
        return cipher.doFinal(encrypted);
    }

    private KeyStore.PrivateKeyEntry getKeyPair()
            throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException,
            NoSuchProviderException, InvalidAlgorithmParameterException,
            UnrecoverableEntryException {
        if (!hasKeyPair()) {
            generateKeyPair();
        }
        return (KeyStore.PrivateKeyEntry) getKeyStore().getEntry(SECRET_KEY_ALIAS, null);
    }

    private boolean hasKeyPair()
            throws CertificateException, NoSuchAlgorithmException, IOException, KeyStoreException {
        return getKeyStore().containsAlias(SECRET_KEY_ALIAS);
    }

    private void generateKeyPair() throws NoSuchProviderException, NoSuchAlgorithmException,
            InvalidAlgorithmParameterException {
        Calendar start = Calendar.getInstance();
        Calendar end = Calendar.getInstance();
        end.add(Calendar.YEAR, 30);

        final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA",
                ANDROID_KEY_STORE);
        final KeyPairGeneratorSpec spec = new KeyPairGeneratorSpec.Builder(mContext)
                .setAlias(SECRET_KEY_ALIAS)
                .setSubject(new X500Principal("CN=" + SECRET_KEY_ALIAS))
                .setSerialNumber(BigInteger.TEN)
                .setStartDate(start.getTime())
                .setEndDate(end.getTime())
                .build();
        keyPairGenerator.initialize(spec);
        keyPairGenerator.generateKeyPair();
    }

}
