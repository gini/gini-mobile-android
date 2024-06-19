package net.gini.android.merchant.sdk.preferences

import android.content.Context
import androidx.core.content.edit

/**
 * Created by Alpár Szotyori on 03.12.21.
 *
 * Copyright (c) 2021 Gini GmbH.
 */
internal class UserPreferences(context: Context) {

    private val sharedPreferences = context.getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)

    fun set(userPreference: UserPreference<*>) {
        userPreference.value?.let { value ->
            sharedPreferences.edit {
                when (value) {
                    is String -> putString(userPreference.id, value)
                    else -> throw IllegalArgumentException("Unknown user preference value type.")
                }
            }
        }
    }

    inline fun <reified T> get(userPreference: UserPreference<T>): UserPreference<T>? {
        if (!sharedPreferences.contains(userPreference.id)) {
            return null
        }

        userPreference.value?.let { value ->
            when (value) {
                is String, (String is T) -> userPreference.value = sharedPreferences.getString(userPreference.id, "") as T
                else -> throw IllegalArgumentException("Unknown user preference value type.")
            }
        }

        return userPreference
    }
}

internal sealed class UserPreference<T>(val id: String, var value: T) {

    class PreferredBankApp(value: String = "") : UserPreference<String>("PreferredBankApp", value)
}