package net.gini.android.models;

import static net.gini.android.helpers.ParcelHelper.doRoundTrip;

import static org.junit.Assert.assertEquals;

import androidx.test.filters.SmallTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class SpecificExtractionTest {

    @Test
    public void testGetName() {
        SpecificExtraction extraction =
                new SpecificExtraction("foo", "bar", "amount", null, new ArrayList<Extraction>());

        assertEquals("foo", extraction.getName());
    }

    @Test
    public void testGetCandidates() {
        ArrayList<Extraction> candidates = new ArrayList<Extraction>();
        candidates.add(new Extraction("0:EUR", "amount", null));
        candidates.add(new Extraction("12.99:EUR", "amount", null));

        SpecificExtraction specificExtraction =
                new SpecificExtraction("amount", "23.23:EUR", "amount", null, candidates);

        assertEquals(candidates, specificExtraction.getCandidate());
    }

    @Test
    public void testIsParcelable() {
        final ArrayList<Extraction> candidates = new ArrayList<Extraction>();
        candidates.add(new Extraction("0:EUR", "amount", null));
        candidates.add(new Extraction("12.99:EUR", "amount", null));
        final Box box = new Box(1, 2, 3, 4, 5);
        SpecificExtraction specificExtraction =
                new SpecificExtraction("amountToPay", "23.23:EUR", "amount", box, candidates);

        final SpecificExtraction restoredExtraction =
                doRoundTrip(specificExtraction, SpecificExtraction.CREATOR);

        assertEquals("amountToPay", restoredExtraction.getName());
        assertEquals("23.23:EUR", restoredExtraction.getValue());
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
