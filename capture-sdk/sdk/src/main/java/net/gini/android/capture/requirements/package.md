# Package net.gini.android.capture.requirements

Contains classes to check if the device meets all the requirements for using the Gini Capture SDK.

Run [net.gini.android.capture.requirements.GiniCaptureRequirements.checkRequirements(android.content.Context)] and check the returned
[net.gini.android.capture.requirements.RequirementsReport] to find out, if the requirements were met. If requirements were not met you can
iterate through the [net.gini.android.capture.requirements.RequirementsReport.getRequirementReports()] and check each
[net.gini.android.capture.requirements.RequirementReport] to find out which requirements were not met.

The checked requirements are listed in the [net.gini.android.capture.requirements.RequirementId] enum.
