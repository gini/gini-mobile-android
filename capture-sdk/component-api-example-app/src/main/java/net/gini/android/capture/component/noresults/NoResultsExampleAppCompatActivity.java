package net.gini.android.capture.component.noresults;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import net.gini.android.capture.Document;
import net.gini.android.capture.component.R;
import net.gini.android.capture.noresults.NoResultsFragmentCompat;
import net.gini.android.capture.noresults.NoResultsFragmentListener;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Created by Alpar Szotyori on 04.12.2017.
 *
 * Copyright (c) 2017 Gini GmbH.
 */

/**
 * AppCompatActivity using the {@link NoResultsScreenHandler} to host the
 * {@link NoResultsFragmentCompat}.
 */
public class NoResultsExampleAppCompatActivity extends AppCompatActivity implements
        NoResultsFragmentListener {

    public static final String EXTRA_IN_DOCUMENT = "EXTRA_IN_DOCUMENT";
    private NoResultsScreenHandler mNoResultsScreenHandler;

    public static Intent newInstance(final Document document, final Context context) {
        final Intent intent = new Intent(context, NoResultsExampleAppCompatActivity.class);
        intent.putExtra(EXTRA_IN_DOCUMENT, document);
        return intent;
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_no_results_compat);
        mNoResultsScreenHandler = new NoResultsScreenHandler(this);
        mNoResultsScreenHandler.onCreate(savedInstanceState);
    }

    @Override
    public void onBackToCameraPressed() {
        mNoResultsScreenHandler.onBackToCameraPressed();
    }
}
