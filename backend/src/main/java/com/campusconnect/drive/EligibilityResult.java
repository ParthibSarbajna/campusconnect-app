package com.campusconnect.drive;

import java.util.List;

/** Outcome of checking one student against one drive. Reasons are student-readable and empty when eligible. */
public record EligibilityResult(boolean eligible, List<String> reasons) {}
