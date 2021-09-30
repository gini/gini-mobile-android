.. _guide-common-tasks

==================
Working with Tasks
==================

The Gini Pay API Library makes heavy use of the concept of tasks. Tasks are convenient when you want to
execute work in succession, each one waiting for the previous to finish (comparable to
Promises in JavaScript). This is a common pattern when working with Gini's remote API. The Gini
Android SDK uses `facebook's task implementation, which is called bolts
<https://github.com/BoltsFramework/Bolts-Android>`_. Before you continue reading this guide, we
strongly encourage you to read the `short guide for the Bolts framework
<https://github.com/BoltsFramework/Bolts-Android/blob/master/Readme.md#tasks>`_.

Upload a document
=================

As the key aspect of the Gini API is to provide information extraction for analyzing documents, the
API is mainly built around the concept of documents. A document can be any written representation of
information such as invoices, reminders, contracts and so on.

The Gini Pay API Library supports creating documents from images, PDFs or UTF-8 encoded text. Images are
usually a picture of a paper document which was taken with the device's camera.

The following example shows how to create a new document from a byte array containing a JPEG image.

.. code-block:: java

    import net.gini.android.Gini;
    import net.gini.android.DocumentTaskManager;
    import net.gini.android.models.Document;
    
    ...
    
    // Assuming that `gini` is an instance of the `Gini` facade class and `imageBytes`
    // is an instance of a byte array containing a JPEG image, 
    // e.g. from a picture taken by the camera
    
    DocumentTaskManager documentTaskManager = gini.getDocumentTaskManager();
    documentTaskManager.createPartialDocument(imageBytes, "image/jpeg", "myFirstDocument.jpg", null)
        .onSuccess(new Continuation<Document, Void>() {
            @Override
            public Void then(Task<Document> task) throws Exception {
                Document document = task.getResult();
                return null;
            }
        });

Each page of a document needs to uploaded as a partial document. In addition documents consisting of
one page also should be uploaded as a partial document.

.. note::

    PDFs and UTF-8 encoded text should also be uploaded as partial documents. Even though PDFs might
    contain multiple pages and text is "pageless", creating partial documents for these keeps your
    interaction with Gini consistent for all the supported document types.

Extractions are not available for partial documents. Creating a partial document is analogous to an
upload. For retrieving extractions see :ref:`getting-extractions`.

.. note::
    
    The filename (``myFirstDocument.jpg`` in the example) is not required, it could be ``null``, but
    setting a filename is a good practice for human readable document identification.

Setting the document type hint
------------------------------

To easily set the document type hint we introduced the ``DocumentType`` enum. It is safer and easier
to use than a ``String``. For more details about the document type hints see the `Document Type
Hints in the Gini API documentation
<https://pay-api.gini.net/documentation/#document-types>`_.

.. _getting-extractions:

Getting extractions
===================

After you have successfully created the partial documents, you most likely want to get the
extractions for the document. *Composite documents* consist of
previously created *partial documents*. You can consider creating partial documents analogous to
uploading pages of a document and creating a composite document analogous to processing those pages
as a single document.

Before retrieving extractions you need to create a composite document from your partial documents.
The ``createCompositeDocument()`` method accepts either a ``List`` of partial ``Documents`` or a
``LinkedHashMap``. The ``LinkedHashMap`` contains partial ``Documents`` as keys and the user applied
rotation as values. In both cases the order is important and the partial documents should be in the
same order as the pages of the scanned document.

Gini needs to process the composite document first before you can fetch the extractions. Effectively
this means that you won't get any extractions before the composite document is fully processed. The
processing time may vary, usually it is in the range of a couple of seconds, but blurred or slightly
rotated images are known to drasticly increase the processing time. 

The ``DocumentTaskManager`` provides the ``pollDocument`` and ``getAllExtractions`` methods which can be
used to fetch the extractions after the processing of the document is completed. The following
example shows how to achieve this in detail.

.. code-block:: java

        import net.gini.android.Gini;
        import net.gini.android.DocumentTaskManager;
        import net.gini.android.models.Document;
        import net.gini.android.models.SpecificExtraction;
        
        ...
        
        // Assuming that `gini` is an instance of the `Gini` facade class and `partialDocuments` is
        // a list of `Documents` which were returned by `createPartialDocument(...)` calls

        final DocumentTaskManager documentTaskManager = gini.getDocumentTaskManager();
        documentTaskManager.createCompositeDocument(partialDocuments, null)
            .onSuccessTask(
                new Continuation<Document, Task<Document>>() {
                    @Override
                    public Task<Document> then(
                            final Task<Document> task)
                            throws Exception {
                        final Document document = task.getResult();
                        return documentTaskManager.pollDocument(document);
                    }
            })
            .onSuccessTask(new Continuation<Document, Task<Map<String, SpecificExtraction>>>() {
                @Override
                public Object then(Task<Document> task) throws Exception {
                    final Document document = task.getResult();
                    return documentTaskManager.getAllExtractions(document);
                }
            })
            .onSuccess(new Continuation<ExtractionsContainer>, Void>() {
                @Override
                public Void then(Task<ExtractionsContainer> task) {
                    final ExtractionsContainer extractions = task.getResult();
                    final SpecificExtraction amountToPay =
                            extractions.getSpecificExtractions().get("amountToPay");
                    final CompoundExtraction lineItems =
                            extractions.getCompoundExtractions().get("lineItems");
                    // You may use the extractions to fulfill your use-case
                    return null;
                }
            });

Sending feedback
================

Depending on your use case your app probably presents the extractions to the user and offers the
opportunity to correct them. We do our best to prevent errors. You can help improve our service if
your app sends feedback for the extractions Gini delivered. Your app should send feedback only for
the extractions the *user has seen and accepted*. Feedback should be sent for corrected extractions
**and** for *correct extractions*. The code example below shows how to correct extractions and send
feedback.

.. code-block:: java

        final Task<Map<String, SpecificExtraction>> retrievedExtractions // provided
        final Document document // provided

        final Map<String, SpecificExtraction> extractions = retrieveExtractions.getResult();
        // amounTo pay was wrong, we'll correct it
        SpecificExtraction amountToPay = extractions.get("amountToPay");
        amountToPay.setValue("31:00");
        
        // we should send only feedback for extractions we have seen and accepted
        // all extractions we've seen were correct except amountToPay
        Map<String, SpecificExtraction> feedback = new HashMap<String, SpecificExtraction>();
        feedback.put("iban", extractions.get("iban"));
        feedback.put("amountToPay", amountToPay);
        feedback.put("bic", extractions.get("bic"));
        feedback.put("senderName", extractions.get("senderName"));

        final Task<Document> sendFeedback = documentTaskManager.sendFeedbackForExtractions(document, feedback);
        sendFeedback.waitForCompletion();

Report an extraction error to Gini
==================================

If the processing result for a document was not satisfactory for the user, your app can enable your
user the opportunity to report an error directly to Gini. Gini will return an error identifier which
can be used to refer to it towards the Gini support. The user must agree that Gini can use this
document for debugging and error analysis. The code example below shows how to send the error report
to Gini.

.. code-block:: java

        final Document document // provided
        documentTaskManager.reportDocument(document, "short summary", "detailed description");

Handling errors
===================

Currently, the Gini Pay API Library doesn't have intelligent error-handling mechanisms. All errors that
occur during executing a task are handed over transparently. You can react on those errors in the
``onError(...)`` method of the task. We may add better error-handling mechanisms in the future. At
the moment we recommend checking the network status when a task failed and retrying the task.
