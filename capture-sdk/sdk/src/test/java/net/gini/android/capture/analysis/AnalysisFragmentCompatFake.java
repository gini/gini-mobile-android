package net.gini.android.capture.analysis;

import net.gini.android.capture.test.FragmentImplFactory;

/**
 * Created by Alpar Szotyori on 15.05.2019.
 *
 * Copyright (c) 2019 Gini GmbH.
 */
public class AnalysisFragmentCompatFake extends AnalysisFragment {

    public static FragmentImplFactory<AnalysisFragmentImpl, AnalysisFragment>
            sFragmentImplFactory;

    public AnalysisFragmentCompatFake() {
    }

    @Override
    AnalysisFragmentImpl createFragmentImpl() {
        return sFragmentImplFactory.createFragmentImpl(this);
    }
}
