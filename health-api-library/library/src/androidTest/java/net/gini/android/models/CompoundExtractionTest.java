package net.gini.android.models;

import static net.gini.android.helpers.ParcelHelper.doRoundTrip;

import static org.junit.Assert.assertEquals;

import androidx.test.filters.SmallTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class CompoundExtractionTest {

    @Test
    public void testIsParcelable() {
        final Box box = new Box(1, 2, 3, 4, 5);

        final List<Map<String, SpecificExtraction>> rows = new ArrayList<>();

        final Map<String, SpecificExtraction> firstRowColumns = new HashMap<>();
        firstRowColumns.put("description", new SpecificExtraction("description", "CORE ICON - Sweatjacke - emerald", "text", box,
                Collections.<Extraction>emptyList()));
        firstRowColumns.put("grossPrice",
                new SpecificExtraction("grossPrice", "39.99:EUR", "amount", box, Collections.<Extraction>emptyList()));
        rows.add(firstRowColumns);

        final Map<String, SpecificExtraction> secondRowColumns = new HashMap<>();
        secondRowColumns.put("description",
                new SpecificExtraction("description", "Strickpullover - yellow", "text", box, Collections.<Extraction>emptyList()));
        secondRowColumns.put("grossPrice",
                new SpecificExtraction("grossPrice", "59.99:EUR", "amount", box, Collections.<Extraction>emptyList()));
        rows.add(secondRowColumns);

        final CompoundExtraction compoundExtraction = new CompoundExtraction("lineItems", rows);

        final CompoundExtraction restoredExtraction =
                doRoundTrip(compoundExtraction, CompoundExtraction.CREATOR);

        assertEquals("lineItems", restoredExtraction.getName());

        assertEquals("CORE ICON - Sweatjacke - emerald", restoredExtraction.getSpecificExtractionMaps().get(0).get(
                "description").getValue());
        assertEquals("39.99:EUR", restoredExtraction.getSpecificExtractionMaps().get(0).get("grossPrice").getValue());

        assertEquals("Strickpullover - yellow", restoredExtraction.getSpecificExtractionMaps().get(1).get("description").getValue());
        assertEquals("59.99:EUR", restoredExtraction.getSpecificExtractionMaps().get(1).get("grossPrice").getValue());
    }
}
