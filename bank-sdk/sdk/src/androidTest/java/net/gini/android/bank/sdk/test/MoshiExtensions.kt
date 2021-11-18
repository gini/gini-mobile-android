package net.gini.android.bank.sdk.test

import android.content.Context
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import com.squareup.moshi.Moshi

/**
 * Created by Alpár Szotyori on 16.11.21.
 *
 * Copyright (c) 2021 Gini GmbH.
 */

inline fun <reified T> Moshi.fromJsonAsset(fileName: String) = getApplicationContext<Context>().resources
    .assets.open(fileName).use {
        adapter(T::class.java).fromJson(String(it.readBytes()))
    }