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
public class ExtractionsContainerTest {

    @Test
    public void testIsParcelable() {
        final ArrayList<Extraction> candidates = new ArrayList<Extraction>();
        candidates.add(new Extraction("0:EUR", "amount", null));
        candidates.add(new Extraction("12.99:EUR", "amount", null));
        final Box box = new Box(1, 2, 3, 4, 5);
        SpecificExtraction specificExtraction =
                new SpecificExtraction("amountToPay", "23.23:EUR", "amount", box, candidates);

        final List<Map<String, SpecificExtraction>> rows = new ArrayList<>();
        final Map<String, SpecificExtraction> row = new HashMap<>();
        row.put("description", new SpecificExtraction("description", "CORE ICON - Sweatjacke - emerald", "text", box,
                Collections.<Extraction>emptyList()));
        row.put("grossPrice", new SpecificExtraction("grossPrice", "39.99:EUR", "amount", box, Collections.<Extraction>emptyList()));
        rows.add(row);
        final CompoundExtraction compoundExtraction = new CompoundExtraction("lineItems", rows);

        final List<ReturnReason> returnReasons = new ArrayList<>();
        final Map<String, String> localizedLabels = new HashMap<>();
        localizedLabels.put("de", "I mogs net");
        localizedLabels.put("en", "It stinks");
        returnReasons.add(new ReturnReason("r1", localizedLabels));

        final ExtractionsContainer extractions = new ExtractionsContainer(
                Collections.singletonMap("amountToPay", specificExtraction),
                Collections.singletonMap("lineItems", compoundExtraction),
                returnReasons
        );

        final ExtractionsContainer restoredExtractions =
                doRoundTrip(extractions, ExtractionsContainer.CREATOR);

        assertEquals("amountToPay", restoredExtractions.getSpecificExtractions().get("amountToPay").getName());
        assertEquals("lineItems", restoredExtractions.getCompoundExtractions().get("lineItems").getName());
        assertEquals("CORE ICON - Sweatjacke - emerald", restoredExtractions.getCompoundExtractions().get(
                "lineItems").getSpecificExtractionMaps().get(0).get("description").getValue());
        assertEquals("I mogs net", restoredExtractions.getReturnReasons().get(0).getLocalizedLabels().get("de"));
    }
}
