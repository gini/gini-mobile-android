package net.gini.android;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class MediaTypes {

    public static final String IMAGE_JPEG = "image/jpeg";
    public static final String GINI_JSON_V1 = "application/vnd.gini.v1+json";
    public static final String GINI_JSON_V2 = "application/vnd.gini.v1+json";
    public static final String GINI_JSON_INCUBATOR = "application/vnd.gini.incubator+json";
    public static final String APPLICATION_JSON = "application/json";
    public static final String APPLICATION_FORM_URLENCODED = "application/x-www-form-urlencoded";
    public static final String GINI_PARTIAL = "application/vnd.gini.v1.partial";
    public static final String GINI_DOCUMENT_JSON_V2 = "application/vnd.gini.v1.composite+json";

    public static String forPartialDocument(@NonNull final String partialMediaType, @NonNull final String mediaType) {
        final String subtype = getSubtype(mediaType);
        return partialMediaType + "+" + subtype;
    }

    @Nullable
    private static String getSubtype(final @NonNull String mediaType) {
        final String[] components = mediaType.split("/");
        if (components.length == 2) {
            return components[1];
        }
        return "";
    }

    private MediaTypes() {

    }
}
