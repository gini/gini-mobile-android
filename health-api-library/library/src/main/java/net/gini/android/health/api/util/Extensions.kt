package net.gini.android.health.api.util

import java.util.*

/**
 * Created by Alpár Szotyori on 27.01.22.
 *
 * Copyright (c) 2022 Gini GmbH.
 */

fun <K, V> Map<out K, V>.toTreeMap(comparator: Comparator<in K>): TreeMap<K, V> =
    TreeMap<K, V>(comparator).apply { putAll(this@toTreeMap) }
