package net.gini.android;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import com.android.volley.Cache;
import com.android.volley.Network;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.BaseHttpStack;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;

import net.gini.android.authorization.PubKeyManager;

import java.io.File;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

import androidx.annotation.XmlRes;

/**
 * <p>
 * Helper class for creating com.android.volley.RequestQueue instances.
 * </p>
 * <p>
 * The default dependencies are taken from the Volley.newRequestQueue() implementation.
 * </p>
 * <p>
 * If no dependency instances were set the builder simply returns Volley.newRequestQueue()'s result.
 * </p>
 */
class RequestQueueBuilder {

    /**
     * Default on-disk cache directory.
     */
    private static final String DEFAULT_CACHE_DIR = "volley";

    private final Context mContext;

    private Cache mCache;
    private String mUserAgent;
    private BaseHttpStack mStack;
    private Network mNetwork;
    private SSLSocketFactory mSSLSocketFactory;
    private List<String> mHostnames;
    @XmlRes
    private int mNetworkSecurityConfigResId;

    RequestQueueBuilder(final Context context) {
        mContext = context;
    }

    public RequestQueueBuilder setHostnames(final List<String> hostnames) {
        mHostnames = hostnames;
        return this;
    }

    RequestQueueBuilder setNetworkSecurityConfigResId(final int networkSecurityConfigResId) {
        mNetworkSecurityConfigResId = networkSecurityConfigResId;
        return this;
    }

    RequestQueueBuilder setCache(final Cache cache) {
        mCache = cache;
        return this;
    }

    RequestQueue build() {
        RequestQueue queue = new RequestQueue(getCache(), getNetwork());
        queue.start();
        return queue;
    }

    private Cache getCache() {
        if (mCache == null) {
            File cacheDir = new File(mContext.getCacheDir(), DEFAULT_CACHE_DIR);
            mCache = new DiskBasedCache(cacheDir);
        }
        return mCache;
    }

    private String getUserAgent() {
        if (mUserAgent == null) {
            mUserAgent = "volley/0";
            try {
                String packageName = mContext.getPackageName();
                PackageInfo info = mContext.getPackageManager().getPackageInfo(packageName, 0);
                mUserAgent = packageName + "/" + info.versionCode;
            } catch (PackageManager.NameNotFoundException ignore) {
            }
        }
        return mUserAgent;
    }

    private BaseHttpStack getStack() {
        if (mStack == null) {
            mStack = getHurlStack();
        }
        return mStack;
    }

    private HurlStack getHurlStack() {
        SSLSocketFactory sslSocketFactory = getSSLSocketFactory();
        if (sslSocketFactory != null) {
            return new HurlStack(null, sslSocketFactory);
        }
        return new HurlStack();
    }

    private SSLSocketFactory getSSLSocketFactory() {
        if (mSSLSocketFactory == null) {
            try {
                final SSLContext sslContext;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Since Android 10 (Q) TLSv1.3 is default
                    // https://developer.android.com/reference/javax/net/ssl/SSLSocket#default-configuration-for-different-android-versions
                    sslContext = SSLContext.getDefault();
                } else {
                    // Force TLSv1.2 on older versions
                    sslContext = SSLContext.getInstance("TLSv1.2");
                }
                sslContext.init(null, getTrustManagers(), null);
                mSSLSocketFactory = sslContext.getSocketFactory();
            } catch (NoSuchAlgorithmException | KeyManagementException ignore) {
            }
        }
        return mSSLSocketFactory;
    }

    private TrustManager[] getTrustManagers() {
        final PubKeyManager.Builder builder = PubKeyManager.builder(mContext);
        if (mHostnames != null && !mHostnames.isEmpty()) {
            builder.setHostnames(mHostnames);
        }
        if (mNetworkSecurityConfigResId != 0) {
            builder.setNetworkSecurityConfigResId(mNetworkSecurityConfigResId);
        }
        if (builder.canBuild()) {
            final PubKeyManager pubKeyManager = builder.build();
            return new TrustManager[]{pubKeyManager};
        }
        return null;
    }

    private Network getNetwork() {
        if (mNetwork == null) {
            mNetwork = new BasicNetwork(getStack());
        }
        return mNetwork;
    }
}
