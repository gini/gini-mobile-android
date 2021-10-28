package net.gini.android.capture.requirements;

import androidx.annotation.NonNull;

interface Requirement {

    @NonNull
    RequirementId getId();

    @NonNull
    RequirementReport check();
}
