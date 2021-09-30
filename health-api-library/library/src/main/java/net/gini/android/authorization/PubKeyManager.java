package net.gini.android.authorization;

import android.content.Context;
import android.os.Build;

import com.datatheorem.android.trustkit.TrustKit;
import com.datatheorem.android.trustkit.config.DomainPinningPolicy;
import com.datatheorem.android.trustkit.config.PublicKeyPin;

import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import androidx.annotation.NonNull;
import androidx.annotation.XmlRes;


public final class PubKeyManager implements X509TrustManager {

    private final Context mContext;
    private final List<String> mHostnames;
    @XmlRes
    private final int mNetworkSecurityConfigResId;
    private Set<PublicKeyPin> mLocalPublicKeys;
    private final List<X509TrustManager> mTrustKitTrustManagers = new ArrayList<>();

    public static Builder builder(@NonNull final Context context) {
        return new Builder(context);
    }

    private PubKeyManager(@NonNull Context context, @NonNull final List<String> hostnames,
            @XmlRes final int networkSecurityConfigResId) {
        mContext = context;
        mHostnames = hostnames;
        mNetworkSecurityConfigResId = networkSecurityConfigResId;
        setupTrustKit();
    }

    private void setupTrustKit() {
        try {
            TrustKit.initializeWithNetworkSecurityConfiguration(mContext,
                    mNetworkSecurityConfigResId);
        } catch (IllegalStateException ignore) {
        }
        mLocalPublicKeys = getPublicKeys(TrustKit.getInstance());
        mTrustKitTrustManagers.clear();
        for (final String hostname : mHostnames) {
            mTrustKitTrustManagers.add(TrustKit.getInstance().getTrustManager(hostname));
        }
    }

    private Set<PublicKeyPin> getPublicKeys(final TrustKit trustKit) {
        final Set<PublicKeyPin> publicKeys = new HashSet<>();
        for (final String hostname : mHostnames) {
            final DomainPinningPolicy pinningPolicy =
                    trustKit.getConfiguration().getPolicyForHostname(hostname);
            if (pinningPolicy != null) {
                publicKeys.addAll(pinningPolicy.getPublicKeyPins());
            }
        }
        return publicKeys;
    }

    @Override
    public void checkServerTrusted(X509Certificate[] remoteX509Certificates, String authType)
            throws CertificateException {
        // TrustKit supports API Levels 17+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            for (final X509TrustManager trustKitTrustManager : mTrustKitTrustManagers) {
                trustKitTrustManager.checkServerTrusted(remoteX509Certificates, authType);
            }
        } else {
            // Fall back to our implementation on API Levels 16-
            if (remoteX509Certificates == null) {
                throw new IllegalArgumentException(
                        "checkServerTrusted: Remote X509Certificate array is null");
            }
            if (remoteX509Certificates.length == 0) {
                throw new IllegalArgumentException(
                        "checkServerTrusted: Remote X509Certificate array is empty");
            }

            checkSSLTLS(remoteX509Certificates, authType);

            boolean trusted = false;
            for (X509Certificate remoteCert : remoteX509Certificates) {
                if (isValidCertificate(remoteCert)) {
                    trusted = true;
                    break;
                }
            }

            if (!trusted) {
                throw new CertificateException("Remote certificate not trusted");
            }
        }
    }

    private void checkSSLTLS(X509Certificate[] certificates, String authType)
            throws CertificateException {
        TrustManagerFactory trustManagerFactory;
        try {
            trustManagerFactory = TrustManagerFactory.getInstance("X509");
            trustManagerFactory.init((KeyStore) null);

            for (TrustManager trustManager : trustManagerFactory.getTrustManagers()) {
                ((X509TrustManager) trustManager).checkServerTrusted(
                        certificates, authType);
            }

        } catch (Exception e) {
            throw new CertificateException(e.toString());
        }
    }

    private Boolean isValidCertificate(final X509Certificate remoteCertificate)
            throws CertificateException {
        final PublicKeyPin remotePublicKey = new PublicKeyPin(remoteCertificate);
        for (final PublicKeyPin localPublicKey : mLocalPublicKeys) {
            if (localPublicKey.equals(remotePublicKey)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void checkClientTrusted(X509Certificate[] x509Certificates, String s)
            throws CertificateException {
        if (!mTrustKitTrustManagers.isEmpty()) {
            for (final X509TrustManager trustKitTrustManager : mTrustKitTrustManagers) {
                trustKitTrustManager.checkClientTrusted(x509Certificates, s);
            }
            return;
        }
        throw new CertificateException("Client certificates not supported!");
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        if (!mTrustKitTrustManagers.isEmpty()) {
            final List<X509Certificate> acceptedIssuers = new ArrayList<>();
            for (final X509TrustManager trustKitTrustManager : mTrustKitTrustManagers) {
                acceptedIssuers.addAll(Arrays.asList(trustKitTrustManager.getAcceptedIssuers()));
            }
            return acceptedIssuers.toArray(new X509Certificate[acceptedIssuers.size()]);
        }
        return new X509Certificate[0];
    }

    public static class Builder {

        private Context mContext;
        private List<String> mHostnames;
        @XmlRes
        private int mNetworkSecurityConfigResId;

        Builder(@NonNull final Context context) {
            mContext = context;
        }

        public Builder setHostnames(final List<String> hostnames) {
            mHostnames = hostnames;
            return this;
        }

        public Builder setNetworkSecurityConfigResId(@XmlRes final int networkSecurityConfigResId) {
            mNetworkSecurityConfigResId = networkSecurityConfigResId;
            return this;
        }

        public boolean canBuild() {
            return mContext != null
                    && mHostnames != null
                    && mNetworkSecurityConfigResId != 0;
        }

        public PubKeyManager build() {
            if (canBuild()) {
                return new PubKeyManager(mContext, mHostnames, mNetworkSecurityConfigResId);
            }
            throw new IllegalArgumentException(
                    "Cannot create PubKeyManager: no local certificates or public keys or network security config was set.");
        }
    }
}