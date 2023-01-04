package net.gini.android.capture.tracking;

/**
 * Created by Alpar Szotyori on 03.03.2020.
 *
 * Copyright (c) 2020 Gini GmbH.
 */

import android.app.Activity;

/**
 * Events triggered on the review screen.
 *
 * <p> If you use the Screen API all events will be triggered automatically.
 *
 */
public enum ReviewScreenEvent {
    /**
     * Triggers when the user presses back.
     */
    BACK,
    /**
     * Triggers when the user presses the next button.
     */
    NEXT,
    /**
     * Triggers when upload failed.
     *
     * <p> Use the keys in {@link ReviewScreenEvent.UPLOAD_ERROR_DETAILS_MAP_KEY} to get details about the event from the details map.
     */
    UPLOAD_ERROR;

    /**
     * Keys to retrieve details about the {@link ReviewScreenEvent#UPLOAD_ERROR} event.
     */
    public static class UPLOAD_ERROR_DETAILS_MAP_KEY {

        /**
         * Error message key in the details map. Value type is {@link String}.
         */
        public static String MESSAGE = "MESSAGE";

        /**
         * Error object key in the details map. Value type is {@link Throwable}.
         */
        public static String ERROR_OBJECT = "ERROR_OBJECT";
    }
}
