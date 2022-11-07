package net.gini.android.core.api;

import android.net.Uri;

import java.nio.charset.Charset;
import java.util.Map;

public class Utils {

    private Utils(){
    }

    /**
     * Ensures that an object reference passed as a parameter to the calling method is not null.
     *
     * @param reference an object reference
     * @return the non-null reference that was validated
     * @throws NullPointerException if {@code reference} is null
     */
    public static <T> T checkNotNull(T reference) {
        if (reference == null) {
            throw new NullPointerException();
        }
        return reference;
    }

    public static Charset CHARSET_UTF8 = Charset.forName("utf-8");
}
