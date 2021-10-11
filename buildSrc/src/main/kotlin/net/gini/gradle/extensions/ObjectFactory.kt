package net.gini.gradle.extensions

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property

/**
 * Created by Alpár Szotyori on 07.10.21.
 *
 * Copyright (c) 2021 Gini GmbH.
 */

// Gratefully taken from: https://github.com/JLLeitschuh/ktlint-gradle/blob/master/plugin/src/main/kotlin/org/jlleitschuh/gradle/ktlint/PluginUtil.kt#L96
internal inline fun <reified T> ObjectFactory.property(
    configuration: Property<T>.() -> Unit = {}
) = property(T::class.java).apply(configuration)

internal inline fun <reified K, reified V> ObjectFactory.mapProperty(
    configuration: MapProperty<K, V>.() -> Unit = {}
) = mapProperty(K::class.java, V::class.java).apply(configuration)