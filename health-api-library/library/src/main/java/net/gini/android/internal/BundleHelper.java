package net.gini.android.internal;

import android.os.Bundle;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Alpar Szotyori on 13.02.2020.
 *
 * Copyright (c) 2020 Gini GmbH.
 */
public class BundleHelper {

    public static <V extends Parcelable> Bundle mapToBundle(Map<String, V> map) {
        final Bundle bundle = new Bundle();
        for (final Map.Entry<String, V> entry : map.entrySet()) {
            bundle.putParcelable(entry.getKey(), entry.getValue());
        }
        return bundle;
    }

    public static <V extends Parcelable> List<Bundle> mapListToBundleList(List<Map<String, V>> mapList) {
        final List<Bundle> bundle = new ArrayList<>(mapList.size());
        for (final Map<String, V> specificExtractionMap : mapList) {
            bundle.add(mapToBundle(specificExtractionMap));
        }
        return bundle;
    }

    public static <V extends Parcelable> HashMap<String, V> bundleToMap(Bundle bundle, ClassLoader classLoader){
        bundle.setClassLoader(classLoader);
        return bundleToMap(bundle);
    }

    public static <V extends Parcelable> HashMap<String, V> bundleToMap(Bundle bundle){
        final HashMap<String, V> map = new HashMap<>(bundle.keySet().size());
        for (final String key : bundle.keySet()) {
            map.put(key, bundle.<V>getParcelable(key));
        }
        return map;
    }

    public static <V extends Parcelable> List<Map<String, V>> bundleListToMapList(List<Bundle> bundleList, ClassLoader classLoader) {
        final List<Map<String, V>> mapList = new ArrayList<>(bundleList.size());
        for (final Bundle bundle : bundleList) {
            mapList.add(BundleHelper.<V>bundleToMap(bundle, classLoader));
        }
        return mapList;
    }

}
