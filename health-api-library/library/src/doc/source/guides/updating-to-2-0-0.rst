Updating to 2.0.0
=================

..
  Audience: Android dev who has integrated 1.0.0
  Purpose: Describe what is new in 2.0.0 and how to migrate from 1.0.0 to 2.0.0
  Content type: Procedural - How-To

  Headers:
  h1 =====
  h2 -----
  h3 ~~~~~
  h4 +++++
  h5 ^^^^^

In version 2.0.0 we removed Bolts and Volley. Bolts was replaced with kotlin coroutines which is part of the kotlin
standard library. Volley was replaced with Retrofit2, a more popular and elegant networking library built upon okhttp3.

We also modernized the internal architecture to take advantage of the features provided by kotlin, coroutines and
Retrofit2.

Overview of public API changes
------------------------------

We tried to minimize the public API changes as much as possible, but due to the fact that Bolts has been part of the old
public API we had to change all methods that exposed Bolts classes. Also some Volley features and classes were exposed
which had to be removed or changed due to the switch to Retrofit2.

GiniHealthAPIBuilder
~~~~~~~~~~~~~~~~~~

The ``GiniHealthAPIBuilder`` was ported to kotlin and the following methods had to be changed or removed: 

- ``setCache()`` now takes an ``okhttp3.Cache`` instead of ``com.android.volley.Cache``. Please consult the `okhttp3
  documentation <https://square.github.io/okhttp/features/caching/>`_ on how to customize caching.
- ``setConnectionBackOffMultiplier()`` was removed as it's not available in Retrofit2 and okhttp3.
- ``setMaxNumberOfRetries()`` was removed as it's not available in Retrofit2 and okhttp3.

GiniHealthAPI
~~~~~~~~~~~

The ``GiniHealthAPI`` which is created via the builder was ported to kotlin and the following methods had to be changed or
removed:

- ``getDocumentTaskManager()`` was removed. The ``getDocumentManager()`` method fully replaces it.

HealthApiDocumentTaskManager
~~~~~~~~~~~~~~~~~~~~~~~~~~

The ``HealthApiDocumentTaskManager`` relied on Bolts and was replaced by the ``HealthApiDocumentManager`` which exposes
kotlin suspend functions instead of Bolts tasks.

HealthApiDocumentManager
~~~~~~~~~~~~~~~~~~~~~~

The ``HealthApiDocumentManager`` is now the main class to execute Gini Health API calls. 

All methods are suspend functions and return an instance of the sealed class ``Resource`` which encapsulates the API
resource and request result. You can find more details in the Resource_ section.

The following methods were renamed, removed or added:

- ``getExtractions()`` was renamed to ``getAllExtractionsWithPolling()``.
- ``getAllExtractions()`` was added to retrieve extractions without polling. This method should be called only after
  polling if the document processing state is ``Document.ProcessingState.COMPLETED``.
- ``reportDocument()`` was removed.
- ``sendFeedback()`` was renamed to ``sendFeedbackForExtractions()`` which has an overload for sending feedback only for
  specific extractions.
- ``logErrorEvent()`` was added to allow reporting errors to the Gini Health API.

SessionManager
~~~~~~~~~~~~~~

The ``SessionManager`` interface was changed to declare the ``getSession()`` method as a suspend function with a return
type of ``Resource<Session>``. You can find more details about the ``Resource`` class in the Resource_ section.

Resource
~~~~~~~~

Instances of the new ``Resource`` sealed class are returned by all methods which previously either returned Bolts tasks
or were suspend functions.

Depending on the request result the following ``Resource`` instances are returned:

- successful request: ``Resource.Success`` which contains the deserialized response payload in the ``data`` property.
- failed request: ``Resource.Error`` which contains the response details and/or the exception which caused the failure.
- cancelled request: ``Resource.Cancelled``.

``Resource`` also provides a helper instance method for chaining requests called ``mapSuccess()``. For more details please
consult the :root_dokka_path:`reference documentation <library/net.gini.android.core.api/-resource/index.html>`.

DocumentType
~~~~~~~~~~~~

The ``DocumentType`` enum was moved from ``DocumentTaskManager`` to ``DocumentManager``. You have to update import
statements to use ``DocumentManager.DocumentType``.

Examples
--------

In this section you can find examples of what needs to be changed for different use cases.

Custom caching
~~~~~~~~~~~~~~

In version 1.x.x you had to pass an implementation of the ``com.android.volley.Cache`` interface to the builder:

.. code-block:: java

    val giniHealthApi: GiniHealthAPI = GiniHealthAPIBuilder(context, "gini-client-id", "GiniClientSecret", "example.com")
        .setCache(CustomVolleyCache())
        .build();

In version 2.x.x you have to pass an ``okhttp3.Cache`` instance:

.. code-block:: java

    val giniHealthApi: GiniHealthAPI = GiniHealthAPIBuilder(getContext(), "gini-client-id", "GiniClientSecret", "example.com")
        .setCache(Cache(
            directory = File(application.cacheDir, "http_cache"),
            maxSize = 50L * 1024L * 1024L // 50 MiB
        ))
        .build();

Custom SessionManager
~~~~~~~~~~~~~~~~~~~~~~

In version 1.x.x you had to return a Bolts ``Task<Session>`` in your ``SessionManager`` interface implementation:

.. code-block:: java

    class CustomSessionManager : SessionManager {
      override fun getSession(): Task<Session> {
          // retrieve a user session
          val session: Session = ...
          return Task.forResult(session)
      }
    }

In version 2.x.x you have to return a ``Resource`` from the ``getSession()`` suspend function:

.. code-block:: java

    class CustomSessionManager : SessionManager {
        override suspend fun getSession(): Resource<Session> {
            // retrieve a user session
            val session: Session = ...
            return Resource.Success(session)
        }
    }

Upload and analyze a document
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

In version 1.x.x to upload and analyze document you could use methods of the ``HealthApiDocumentTaskManager`` which
returned Bolts tasks:

.. code-block:: java

    // This example uses Java

    // Assuming that `giniHealthApi` is an instance of the `GiniHealthAPI` facade class

    // Upload and analysis requires creation of a partial document and then a composite document

    // Create a partial document by uploading the document bytes
    giniHealthApi.getDocumentTaskManager()
            .createPartialDocument(documentBytes, "image/jpeg", null, null)
            .onSuccessTask(new Continuation<Document, Task<Document>>() {
                @Override
                public Task<Document> then(Task<Document> task) throws Exception {
                    final Document partialDocument = task.getResult();
                    
                    // Create a composite document to start analysis
                    return giniHealthApi.getDocumentTaskManager().createCompositeDocument(Collections.singletonList(partialDocument), null);
                }
            })
            .onSuccessTask(new Continuation<Document, Task<Document>>() {
                @Override
                public Task<Document> then(Task<Document> task) throws Exception {
                    final Document compositeDocument = task.getResult();

                    // Poll the document processing state
                    return giniHealthApi.getDocumentTaskManager().pollDocument(compositeDocument);
                }
            })
            .onSuccessTask(new Continuation<Document, Task<ExtractionsContainer>>() {

                @Override
                public Task<ExtractionsContainer> then(Task<Document> task) throws Exception {
                    final Document compositeDocument = task.getResult();

                    // Retrieve the extractions
                    return giniHealthApi.getDocumentTaskManager().getAllExtractions(compositeDocument);
                }
            })
            .continueWith(new Continuation<ExtractionsContainer, Void>() {
                @Override
                public Void then(Task<ExtractionsContainer> task) throws Exception {
                    if (task.isFaulted()) {
                        // Handle error
                        final Exception e = task.getError();
                    } else {
                        // Use the extractions
                        final ExtractionsContainer extractionsContainer = task.getResult();
                    }
                    return null;
                }
            });

In version 1.x.x you were also able to complete the above with coroutines using the ``HealthApiDocumentManager``:

.. code-block:: java

    // Assuming that `giniHealthApi` is an instance of the `GiniHealthAPI` facade class

    // Upload and analysis requires creation of a partial document and then a composite document

    coroutineScope.launch {
        // Create a partial document by uploading the document data
        val partialDocument = giniHealthApi.documentManager.createPartialDocument(documentBytes, "image/jpeg")

        // Create a composite document to start analysis
        val compositeDocument = giniHealthApi.documentManager.createCompositeDocument(listOf(partialDocument))

        // Poll the document and retrieve the extractions
        val extractions = giniHealthApi.documentManager.getExtractions(compositeDocument)
    }

In version 2.x.x you have to use the ``HealthApiDocumentManager`` which returns ``Resource`` instances:

.. code-block:: java

    // Assuming that `giniHealthApi` is an instance of the `GiniHealthAPI` facade class

    // Upload and analysis requires creation of a partial document and then a composite document

    coroutineScope.launch {
        // Create a partial document by uploading the document data
        val extractionsResource =
            giniHealthApi.documentManager.createPartialDocument(documentBytes, "image/jpeg")
                .mapSuccess { partialDocumentResource ->
                    // Create a composite document to start analysis
                    giniHealthApi.documentManager.createCompositeDocument(listOf(partialDocumentResource.data))
                }
                .mapSuccess { compositeDocumentResource ->
                    // Poll the document and retrieve the extractions
                    giniHealthApi.documentManager.getAllExtractionsWithPolling(compositeDocumentResource.data)
                }

        when (extractionsResource) {
            is Resource.Success -> {
                // You may use the extractions to fulfill your use-case
                val extractionsContainer = extractionsResource.data
            }
            is Resource.Error -> // Handle error
            is Resource.Cancelled -> // Handle cancellation
        }
    }

Instead of using ``mapExtractions()`` you could also use ``when`` for each returned ``Resource`` to handle errors and
cancellations for each request separately.