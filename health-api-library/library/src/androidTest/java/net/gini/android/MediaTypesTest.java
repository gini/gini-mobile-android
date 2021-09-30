package net.gini.android;

import static org.junit.Assert.assertEquals;

import androidx.test.filters.SmallTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class MediaTypesTest {

    @Test
    public void testGINI_JSON_V1() {
        assertEquals("application/vnd.gini.v1+json", MediaTypes.GINI_JSON_V1);
    }

    @Test
    public void testGINI_JSON_V2() {
        assertEquals("application/vnd.gini.v1+json", MediaTypes.GINI_JSON_V2);
    }

    @Test
    public void testGINI_JSON_INCUBATOR() {
        assertEquals("application/vnd.gini.incubator+json", MediaTypes.GINI_JSON_INCUBATOR);
    }

    @Test
    public void testAPPLICATION_JSON() {
        assertEquals("application/json", MediaTypes.APPLICATION_JSON);
    }

    @Test
    public void testAPPLICATION_FORM_URLENCODED() {
        assertEquals("application/x-www-form-urlencoded", MediaTypes.APPLICATION_FORM_URLENCODED);
    }

    @Test
    public void testIMAGE_JPEG() {
        assertEquals("image/jpeg", MediaTypes.IMAGE_JPEG);
    }
}
