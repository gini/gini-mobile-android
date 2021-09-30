package net.gini.android.models;

import static net.gini.android.helpers.ParcelHelper.doRoundTrip;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.test.filters.SmallTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class ExtractionTest {

    public Box createEmptyBox() {
        return new Box(1, 0, 0, 0, 0);
    }

    @Test
    public void testExtractionIsInitiallyNotMarkedAsDirty() {
        Extraction extraction = new Extraction("bar", "amount", null);

        assertFalse(extraction.isDirty());
    }

    @Test
    public void testGetValue() {
        Extraction extraction = new Extraction("bar", "amount", null);

        assertEquals("bar", extraction.getValue());
    }

    @Test
    public void testGetEntity() {
        Extraction extraction = new Extraction("bar", "amount", null);

        assertEquals("amount", extraction.getEntity());
    }

    @Test
    public void testThatSetValueMarksAsDirty() {
        Extraction extraction = new Extraction("bar", "amount", null);

        extraction.setValue("raboof");

        assertEquals("raboof", extraction.getValue());
        assertTrue(extraction.isDirty());
    }

    @Test
    public void testThatSetBoxMarksAsDirty() {
        Extraction extraction = new Extraction("bar", "amount", null);
        Box newBox = createEmptyBox();

        extraction.setBox(newBox);

        assertEquals(newBox, extraction.getBox());
        assertTrue(extraction.isDirty());
    }

    @Test
    public void testIsParcelable() {
        final Box box = new Box(1, 2, 3, 4, 5);
        final Extraction originalExtraction = new Extraction("42:EUR", "amount", box);

        final Extraction restoredExtraction = doRoundTrip(originalExtraction, Extraction.CREATOR);

        assertEquals("42:EUR", restoredExtraction.getValue());
        assertEquals("amount", restoredExtraction.getEntity());
        final Box restoredBox = restoredExtraction.getBox();
        // TODO: custom equals on the box model.
        assertEquals(1, restoredBox.getPageNumber());
        assertEquals(2., restoredBox.getLeft(), 0);
        assertEquals(3., restoredBox.getTop(), 0);
        assertEquals(4., restoredBox.getWidth(), 0);
        assertEquals(5., restoredBox.getHeight(), 0);
    }
}
