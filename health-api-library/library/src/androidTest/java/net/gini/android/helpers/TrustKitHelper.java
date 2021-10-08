package net.gini.android.helpers;

import com.datatheorem.android.trustkit.TrustKit;
import com.datatheorem.android.trustkit.pinning.TrustManagerBuilder;

import java.lang.reflect.Field;

/**
 * Created by Alpar Szotyori on 07.02.2018.
 *
 * Copyright (c) 2018 Gini GmbH.
 */

public final class TrustKitHelper {

    public static void resetTrustKit() throws NoSuchFieldException, IllegalAccessException {
        final Field trustKitInstance = TrustKit.class.getDeclaredField("trustKitInstance");
        trustKitInstance.setAccessible(true);
        trustKitInstance.set(null, null);

        Field baselineTrustManager = TrustManagerBuilder.class.getDeclaredField("baselineTrustManager");
        baselineTrustManager.setAccessible(true);
        baselineTrustManager.set(null, null);
    }

    private TrustKitHelper() {
    }
}
