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

    public static AnalysisViewModel sViewModel;

    public AnalysisFragmentCompatFake() {
        // Required empty public constructor
    }

    @Override
    AnalysisViewModel createViewModel() {
        if (sViewModel != null) {
            return sViewModel;
        }
        return super.createViewModel();
    }

    @Override
    AnalysisFragmentImpl createFragmentImpl() {
        return sFragmentImplFactory.createFragmentImpl(this);
    }
}
