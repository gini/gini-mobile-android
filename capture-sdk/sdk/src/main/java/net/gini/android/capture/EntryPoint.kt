package net.gini.android.capture

/**
 * Created by Alpár Szotyori on 10.07.23.
 *
 * Copyright (c) 2023 Gini GmbH.
 */

/**
 * Use this enum to set which entry point is used for starting the SDK.
 */
enum class EntryPoint {
    /**
     * Must be used when the user launches the SDK from a text field.
     */
    FIELD,

    /**
     * Must be used when the user launches the SDK from a button.
     */
    BUTTON
}