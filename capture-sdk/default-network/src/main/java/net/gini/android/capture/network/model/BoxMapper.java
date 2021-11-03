package net.gini.android.capture.network.model;

import net.gini.android.core.api.models.Box;

import androidx.annotation.Nullable;

/**
 * Created by Alpar Szotyori on 30.01.2018.
 *
 * Copyright (c) 2018 Gini GmbH.
 */

/**
 * Helper class to map the {@link Box} from the Gini Bank API lib to the Gini Capture SDK's {@link
 * GiniCaptureBox} and vice versa.
 */
public final class BoxMapper {

    /**
     * Map a {@link Box} from the Gini Bank API lib to the Gini Capture SDK's {@link GiniCaptureBox}.
     *
     * @param source Gini Bank API lib {@link Box}
     *
     * @return a Gini Capture SDK {@link GiniCaptureBox}
     */
    @Nullable
    public static GiniCaptureBox map(@Nullable final Box source) {
        if (source == null) {
            return null;
        }
        return new GiniCaptureBox(source.getPageNumber(), source.getLeft(), source.getTop(),
                source.getWidth(), source.getHeight());
    }

    /**
     * Map a {@link GiniCaptureBox} from the Gini Capture SDK to the Gini Bank API lib's {@link Box}.
     *
     * @param source Gini Capture SDK {@link GiniCaptureBox}
     *
     * @return Gini Bank API lib {@link Box}
     */
    @Nullable
    public static Box map(@Nullable final GiniCaptureBox source) {
        if (source == null) {
            return null;
        }
        return new Box(source.getPageNumber(), source.getLeft(), source.getTop(), source.getWidth(),
                source.getHeight());
    }

    private BoxMapper() {
    }
}
