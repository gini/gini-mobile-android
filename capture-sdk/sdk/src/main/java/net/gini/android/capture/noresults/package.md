# Package net.gini.android.capture.noresults

Contains the Activity and Fragments used for the No Results Screen. The No Results Screen includes hints on how to take a picture in an
optimal condition. This screen should be shown when the received results from the Gini API don't include the required extractions.

## Screen API

The [net.gini.android.capture.noresults.NoResultsActivity] is launched directly by the [net.gini.android.capture.analysis.AnalysisActivity] or
the [net.gini.android.capture.review.ReviewActivity], depending on where analysis results were received.

Call [net.gini.android.capture.analysis.AnalysisFragmentInterface#onNoExtractionsFound()] or
[net.gini.android.capture.review.ReviewFragmentInterface#onNoExtractionsFound()] after you received the results from the Gini API and it
didn't include the required extractions.

## Component API

To use the Component API you have to include the [net.gini.android.capture.noresults.NoResultsFragmentCompat] 
in an Activity in your app (a dedicated Activity is recommended). To receive events from the Fragments 
your Activity must implement the [net.gini.android.capture.noresults.NoResultsFragmentListener] interface.


