package net.gini.android.models;

import static net.gini.android.helpers.ParcelHelper.doRoundTrip;

import static org.junit.Assert.assertEquals;

import androidx.test.filters.SmallTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class BoxTest {

    @Test
    public void testBoxIsParcelable() {
        final Box originalBox = new Box(1, 2, 3, 4, 5);
        final Box restoredBox = doRoundTrip(originalBox, Box.CREATOR);
        assertEquals(1, restoredBox.getPageNumber());
        assertEquals(2., restoredBox.getLeft(), 0);
        assertEquals(3., restoredBox.getTop(), 0);
        assertEquals(4., restoredBox.getWidth(), 0);
        assertEquals(5., restoredBox.getHeight(), 0);
    }

}
