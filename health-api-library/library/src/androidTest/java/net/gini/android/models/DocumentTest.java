package net.gini.android.models;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;

import static net.gini.android.helpers.ParcelHelper.doRoundTrip;

import static org.junit.Assert.assertEquals;

import android.net.Uri;
import androidx.test.filters.SmallTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class DocumentTest {

    private JSONObject createDocumentJSON() throws IOException, JSONException {
        return createDocumentJSON("document.json");
    }

    private JSONObject createDocumentJSON(final String fileName) throws IOException, JSONException {
        InputStream is = getApplicationContext().getResources().getAssets().open(fileName);

        int size = is.available();
        byte[] buffer = new byte[size];
        //noinspection ResultOfMethodCallIgnored
        is.read(buffer);
        is.close();

        return new JSONObject(new String(buffer));
    }

    @Test
    public void testDocumentIdGetter() {
        Document document = new Document("1234-5678-9012-3456", Document.ProcessingState.COMPLETED,
                "foobar.jpg", 1, new Date(),
                Document.SourceClassification.NATIVE, Uri.parse(""), new ArrayList<Uri>(),
                new ArrayList<Uri>());

        assertEquals("1234-5678-9012-3456", document.getId());
    }

    @Test
    public void testDocumentState() {
        Document document = new Document("1234-5678-9012-3456", Document.ProcessingState.COMPLETED,
                "foobar.jpg", 1, new Date(),
                Document.SourceClassification.NATIVE, Uri.parse(""), new ArrayList<Uri>(),
                new ArrayList<Uri>());

        assertEquals(Document.ProcessingState.COMPLETED, document.getState());
    }

    @Test
    public void testDocumentFactory() throws IOException, JSONException {
        JSONObject responseData = createDocumentJSON();

        Document document = Document.fromApiResponse(responseData);

        assertEquals("626626a0-749f-11e2-bfd6-000000000000", document.getId());
        assertEquals(Document.ProcessingState.COMPLETED, document.getState());
        assertEquals("scanned.jpg", document.getFilename());
        assertEquals(1, document.getPageCount());
        assertEquals(Document.SourceClassification.SCANNED, document.getSourceClassification());
        // Tue Feb 12 2013 00:04:27 GMT+0100 (CET)
        assertEquals(1360623867402L, document.getCreationDate().getTime());
    }

    @Test
    public void testDocumentFactory_withUnknown_sourceClassification() throws IOException, JSONException {
        JSONObject responseData = createDocumentJSON("unknown-source-classification-document.json");

        Document document = Document.fromApiResponse(responseData);

        assertEquals("626626a0-749f-11e2-bfd6-000000000000", document.getId());
        assertEquals(Document.ProcessingState.COMPLETED, document.getState());
        assertEquals("scanned.jpg", document.getFilename());
        assertEquals(1, document.getPageCount());
        assertEquals(Document.SourceClassification.UNKNOWN, document.getSourceClassification());
        // Tue Feb 12 2013 00:04:27 GMT+0100 (CET)
        assertEquals(1360623867402L, document.getCreationDate().getTime());
    }

    @Test
    public void testDocumentFactory_withUnknown_processingState() throws IOException, JSONException {
        JSONObject responseData = createDocumentJSON("unknown-processing-state-document.json");

        Document document = Document.fromApiResponse(responseData);

        assertEquals("626626a0-749f-11e2-bfd6-000000000000", document.getId());
        assertEquals(Document.ProcessingState.UNKNOWN, document.getState());
        assertEquals("scanned.jpg", document.getFilename());
        assertEquals(1, document.getPageCount());
        assertEquals(Document.SourceClassification.TEXT, document.getSourceClassification());
        // Tue Feb 12 2013 00:04:27 GMT+0100 (CET)
        assertEquals(1360623867402L, document.getCreationDate().getTime());
    }

    @Test
    public void testShouldBeParcelable() {
        final Date date = new Date();
        final Document originalDocument =
                new Document("1234-5678-9012-3456", Document.ProcessingState.COMPLETED,
                        "foobar.jpg", 1, date,
                        Document.SourceClassification.NATIVE, Uri.parse(""), new ArrayList<Uri>(),
                        new ArrayList<Uri>());

        final Document restoredDocument = doRoundTrip(originalDocument, Document.CREATOR);

        assertEquals("1234-5678-9012-3456", restoredDocument.getId());
        assertEquals(Document.ProcessingState.COMPLETED, restoredDocument.getState());
        assertEquals("foobar.jpg", restoredDocument.getFilename());
        assertEquals(1, restoredDocument.getPageCount());
        assertEquals(date, restoredDocument.getCreationDate());
        assertEquals(Document.SourceClassification.NATIVE,
                restoredDocument.getSourceClassification());
    }
}
