package net.gini.android.capture.component.noresults;

import static net.gini.android.capture.component.noresults.NoResultsExampleAppCompatActivity.EXTRA_IN_DOCUMENT;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import net.gini.android.capture.Document;
import net.gini.android.capture.component.R;
import net.gini.android.capture.component.camera.CameraExampleAppCompatActivity;
import net.gini.android.capture.noresults.NoResultsFragmentCompat;
import net.gini.android.capture.noresults.NoResultsFragmentListener;

/**
 * Created by Alpar Szotyori on 04.12.2017.
 *
 * Copyright (c) 2017 Gini GmbH.
 */

/**
 * Contains the logic for the No Results Screen.
 */
public class NoResultsScreenHandler implements NoResultsFragmentListener {

    private final AppCompatActivity mActivity;
    private NoResultsFragmentCompat mNoResultsFragment;
    private Document mDocument;

    public NoResultsScreenHandler(final AppCompatActivity activity) {
        mActivity = activity;
    }

    @Override
    public void onBackToCameraPressed() {
        final Intent intent = getCameraActivityIntent();
        mActivity.startActivity(intent);
        mActivity.finish();
    }

    private Intent getCameraActivityIntent() {
        return new Intent(mActivity, CameraExampleAppCompatActivity.class);
    }

    public Activity getActivity() {
        return mActivity;
    }

    public Document getDocument() {
        return mDocument;
    }

    public void onCreate(final Bundle savedInstanceState) {
        setUpActionBar();
        setTitles();
        readExtras();

        if (savedInstanceState == null) {
            showNoResultsFragment();
        } else {
            retainNoResultsFragment();
        }
    }

    private void retainNoResultsFragment() {
        mNoResultsFragment =
                (NoResultsFragmentCompat) mActivity.getSupportFragmentManager()
                        .findFragmentById(R.id.no_results_screen_container);
    }

    private void showNoResultsFragment() {
        mNoResultsFragment = NoResultsFragmentCompat.createInstance(getDocument());
        mActivity.getSupportFragmentManager().beginTransaction()
                .replace(R.id.no_results_screen_container, mNoResultsFragment)
                .commit();
    }

    private void readExtras() {
        mDocument = mActivity.getIntent().getParcelableExtra(EXTRA_IN_DOCUMENT);
    }

    private void setTitles() {
        final ActionBar actionBar = mActivity.getSupportActionBar();
        if (actionBar == null) {
            return;
        }
        actionBar.setTitle(R.string.no_results_screen_title);
        actionBar.setSubtitle(mActivity.getString(R.string.no_results_screen_subtitle));
    }

    private void setUpActionBar() {
        mActivity.setSupportActionBar(
                (Toolbar) mActivity.findViewById(R.id.toolbar));
    }
}
