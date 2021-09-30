package net.gini.android.authorization.crypto;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import androidx.annotation.NonNull;

/**
 * Created by Alpar Szotyori on 09.10.2018.
 *
 * Copyright (c) 2018 Gini GmbH.
 */
public class GiniCryptoHelper {

    public static void deleteSecretKey(@NonNull final GiniCrypto giniCrypto)
            throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException {
        giniCrypto.deleteSecretKey();
    }

}
