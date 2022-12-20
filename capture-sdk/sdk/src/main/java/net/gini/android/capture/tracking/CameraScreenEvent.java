package net.gini.android.capture.tracking;

/**
 * Created by Alpar Szotyori on 03.03.2020.
 *
 * Copyright (c) 2020 Gini GmbH.
 */

/**
 * Events triggered on the camera screen.
 *
 * <p> If you use the Screen API all events will be triggered automatically.
 *
 */
public enum CameraScreenEvent {
    /**
     * Triggers when the user presses back.(<b>Screen API only</b>)
     */
    EXIT,
    /**
     * Triggers when the user opens the help screen.(<b>Screen API only</b>)
     */
    HELP,
    /**
     * Triggers when the user takes a picture.(<b>Screen API</b>)
     */
    TAKE_PICTURE
}
