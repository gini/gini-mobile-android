Error Handling
==============

The Gini Health SDK provides structured, detailed error information for API failures. Errors carry the HTTP
status code, a human-readable message, a support request ID, and — for bulk operations — the list of affected
entity IDs (documents or payment requests).

The SDK error model is built around ``GiniHealthException``, ``ErrorResponse``, and ``ErrorItem``, each
described in detail below.

The ``GiniHealthException`` class
----------------------------------

``GiniHealthException`` extends ``Exception`` and is thrown whenever an API call fails — whether the failure
is an HTTP error response (``statusCode`` and ``errorResponse`` are populated) or a network-level failure such
as a timeout (``cause`` holds the underlying exception, ``statusCode`` and ``errorResponse`` are ``null``).

.. important::

    ``GiniHealthException`` does **not** cover all exceptions that the SDK may throw. The following categories of
    exception are thrown directly as their native types and are **not** wrapped in ``GiniHealthException``:

    * **Coroutine cancellation** — Kotlin's coroutine framework signals cancellation via ``CancellationException``
      (a subclass of ``Exception``). It will be caught by a generic ``catch (e: Exception)`` block — if your
      coroutine scope requires cooperative cancellation, rethrow it manually.
    * **Validation / precondition errors** — e.g. ``IllegalStateException`` when input validation fails
      (for example, calling ``getPaymentFragmentWithoutDocument()`` with incomplete payment details or
      an invalid IBAN).
    * **Programming errors** — e.g. ``NullPointerException`` or ``IllegalArgumentException`` caused by
      internal SDK misconfiguration.

    Always place a general ``catch (e: Exception)`` block **after** your ``catch (e: GiniHealthException)`` block
    to handle these remaining cases.

.. code-block:: kotlin

    open class GiniHealthException(
        message: String,             // Raw message (may be a JSON string — kept for backward compatibility)
        cause: Throwable? = null,    // Underlying exception — e.g. SocketTimeoutException for network failures, ApiException for HTTP errors
        val statusCode: Int? = null, // HTTP status code (e.g., 404, 422, 500)
        val errorResponse: ErrorResponse? = null  // Fully parsed API error body
    ) : Exception(message, cause) {

        // Human-readable message for display — resolves in priority order:
        // 1. errorResponse.message  2. raw message  3. "Unknown error"
        val parsedMessage: String

        // Unique request ID from errorResponse — provide to Gini support when reporting issues
        val requestId: String?

        // Individual error entries from errorResponse.items (code, message, affected entity IDs)
        val errorItems: List<ErrorItem>?
    }


The ``ErrorResponse`` class
----------------------------

``ErrorResponse`` is the data model for the Gini Health API v5.0 error body:

.. code-block:: json

    {
      "message": "Validation of the request entity failed",
      "requestId": "8896f9dc-260d-4133-9848-c54e5715270f",
      "items": [
        {
          "code": "2002",
          "message": "Value too long",
          "object": ["doc-id-1", "doc-id-2"]
        }
      ]
    }

Mapped to Kotlin as:

.. code-block:: kotlin

    data class ErrorResponse(
        val message: String? = null,         // Human-consumable description — not intended for direct end-user display
        val requestId: String? = null,       // Unique request ID — provide to support
        val items: List<ErrorItem>? = null   // Individual error entries
    )

    data class ErrorItem(
        val code: String,                        // Short error code (e.g., "2002")
        val message: String? = null,             // Per-item description
        val affectedIds: List<String>? = null // Affected entity IDs (documents or payment requests)
    )

.. note::

    ``ErrorResponse`` is parsed automatically from the raw HTTP response body. You do not need to parse it yourself.
    It is available via ``GiniHealthException.errorResponse``.

How errors are propagated
--------------------------

Flows
~~~~~

Errors from ``GiniHealth.documentFlow`` and ``GiniHealth.paymentFlow`` arrive as ``ResultWrapper.Error``.
The ``error`` property is a ``GiniHealthException`` when an API call fails, or a plain ``Throwable`` otherwise:

.. code-block:: kotlin

    lifecycleScope.launch {
        giniHealth.documentFlow.collect { result ->
            when (result) {
                is ResultWrapper.Loading -> showProgressIndicator()
                is ResultWrapper.Success -> showDocument(result.value)
                is ResultWrapper.Error -> {
                    val exception = result.error
                    if (exception is GiniHealthException) {
                        Log.e("GiniHealth", "HTTP ${exception.statusCode}: ${exception.parsedMessage}")
                        showError(exception.parsedMessage)
                    } else {
                        showError(exception.message ?: "Unknown error")
                    }
                }
            }
        }
    }

The ``paymentFlow`` and ``trustMarkersFlow`` flows follow the same pattern. For ``openBankState``, errors
are surfaced as ``GiniHealth.PaymentState.Error`` — check its ``throwable`` property with the same
``is GiniHealthException`` cast.

``GiniHealth`` suspend functions
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

All ``GiniHealth`` suspend functions throw ``GiniHealthException`` when an API call fails. Always add a second
``catch`` for cancellation and other non-API failures:

.. code-block:: kotlin

    coroutineScope.launch {
        try {
            val payment = giniHealth.getPayment(paymentRequestId)
        } catch (e: GiniHealthException) {
            // API error — structured details available
            Log.e("GiniHealth", "HTTP ${e.statusCode}: ${e.parsedMessage}")
            Log.e("GiniHealth", "Request ID: ${e.requestId}")
            e.errorItems?.forEach { item ->
                Log.e("GiniHealth", "  Code=${item.code} | Affected: ${item.affectedIds}")
            }
        } catch (e: Exception) {
            // SDK request cancellation, validation error, or CancellationException from coroutine scope
            Log.e("GiniHealth", "Unexpected: ${e.message}")
        }
    }

The same pattern applies to ``deletePaymentRequest()``, ``deletePaymentRequests()``, and ``deleteDocuments()``.

For ``checkIfDocumentIsPayable()`` and ``checkIfDocumentContainsMultipleDocuments()``, the same
``GiniHealthException`` is thrown on API failure — however, **cancellation does not throw**: both
functions return ``false`` when the underlying request is cancelled.

``documentManager`` functions (``Resource`` return type)
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Functions on ``giniHealth.documentManager`` return ``Resource<T>`` directly instead of throwing.
``Resource.Error`` carries the full ``errorResponse``:

.. code-block:: kotlin

    coroutineScope.launch {
        val result = giniHealth.documentManager.createPartialDocument(
            document = imageBytes,
            contentType = "image/jpeg",
            filename = "invoice_page_1.jpg"
        )
        when (result) {
            is Resource.Success -> { /* use result.data */ }
            is Resource.Error -> {
                val errorMsg = result.errorResponse?.message ?: result.message
                Log.e("GiniHealth", "HTTP ${result.responseStatusCode}: $errorMsg")
                Log.e("GiniHealth", "Request ID: ${result.errorResponse?.requestId}")
            }
            is Resource.Cancelled -> { /* handle cancellation */ }
        }
    }

The same pattern applies to ``createCompositeDocument()``, ``getDocument()``,
``getAllExtractionsWithPolling()``, and all other ``documentManager`` calls.

Breaking changes
----------------

Return type changes
~~~~~~~~~~~~~~~~~~~~

The three delete functions **do not return error information** — they return ``Unit`` and throw
``GiniHealthException`` on failure. Any existing code that checked for a non-null return value must be
replaced with a ``try/catch`` block.

+-------------------------------+------------------------------------------+------------------------------------------------------------+
| Method                        | Old return type                          | New behaviour                                              |
+===============================+==========================================+============================================================+
| ``deletePaymentRequest``      | ``String?`` (error message or null)      | ``Unit`` — throws ``GiniHealthException`` (API error) or  |
|                               |                                          | ``Exception`` (cancelled)                                  |
+-------------------------------+------------------------------------------+------------------------------------------------------------+
| ``deletePaymentRequests``     | ``DeletePaymentRequestErrorResponse?``   | ``Unit`` — throws ``GiniHealthException`` (API error) or  |
|                               |                                          | ``Exception`` (cancelled)                                  |
+-------------------------------+------------------------------------------+------------------------------------------------------------+
| ``deleteDocuments``           | ``DeleteDocumentErrorResponse?``         | ``Unit`` — throws ``GiniHealthException`` (API error) or  |
|                               |                                          | ``Exception`` (cancelled)                                  |
+-------------------------------+------------------------------------------+------------------------------------------------------------+

Old API — return value check:

.. code-block:: kotlin

    val error = giniHealth.deletePaymentRequests(ids)
    if (error != null) {
        showError(error.message ?: "Unknown error")
    }

New API — ``try/catch`` block:

.. code-block:: kotlin

    try {
        giniHealth.deletePaymentRequests(ids)
    } catch (e: GiniHealthException) {
        showError(e.parsedMessage)
        // Inspect which IDs caused the failure
        e.errorItems?.forEach { item ->
            Log.e("GiniHealth", "Code=${item.code} | Affected: ${item.affectedIds}")
        }
    } catch (e: Exception) {
        // SDK request cancellation, validation error, or CancellationException from coroutine scope
    }
