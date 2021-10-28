package net.gini.android.capture.analysis;

import android.content.Intent;

import net.gini.android.capture.Document;

public class AnalysisActivityTestSpy extends AnalysisActivity {

    public Intent addDataToResultIntent = null;
    public Document analyzeDocument = null;
    public boolean finishWasCalled = false;

    @Override
    public void finish() {
        finishWasCalled = true;
        super.finish();
    }
}
