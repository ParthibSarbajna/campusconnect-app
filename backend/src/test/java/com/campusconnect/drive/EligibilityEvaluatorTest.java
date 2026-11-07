package com.campusconnect.drive;

import static com.campusconnect.drive.PlacementFixtures.drive;
import static com.campusconnect.drive.PlacementFixtures.profile;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Set;
import org.junit.jupiter.api.Test;

class EligibilityEvaluatorTest {

    private final PlacementDrive d = drive(1, 1, DriveStatus.OPEN); // 2027 batch, CGPA >= 7.50, 0 backlogs, CSE or IT

    @Test
    void studentMeetingEveryCriterionIsEligible() {
        EligibilityResult r = EligibilityEvaluator.evaluate(profile(1, "cse", 2027, "8.20", 0), d);
        assertThat(r.eligible()).isTrue();
        assertThat(r.reasons()).isEmpty();
    }

    @Test
    void missingProfileIsExplained() {
        EligibilityResult r = EligibilityEvaluator.evaluate(null, d);
        assertThat(r.eligible()).isFalse();
        assertThat(r.reasons()).singleElement().asString().contains("Save your student profile");
    }

    @Test
    void wrongGraduationYearIsRejectedWithBothYears() {
        EligibilityResult r = EligibilityEvaluator.evaluate(profile(1, "CSE", 2026, "9.00", 0), d);
        assertThat(r.eligible()).isFalse();
        assertThat(r.reasons()).singleElement().asString().contains("2027").contains("2026");
    }

    @Test
    void cgpaBelowMinimumIsRejectedAndBoundaryIsAccepted() {
        assertThat(EligibilityEvaluator.evaluate(profile(1, "CSE", 2027, "7.49", 0), d).eligible()).isFalse();
        assertThat(EligibilityEvaluator.evaluate(profile(1, "CSE", 2027, "7.50", 0), d).eligible()).isTrue();
    }

    @Test
    void missingCgpaIsRejectedWhenADriveRequiresOne() {
        EligibilityResult r = EligibilityEvaluator.evaluate(profile(1, "CSE", 2027, null, 0), d);
        assertThat(r.eligible()).isFalse();
        assertThat(r.reasons()).singleElement().asString().contains("Add your CGPA");
    }

    @Test
    void backlogsAboveTheLimitAreRejected() {
        EligibilityResult r = EligibilityEvaluator.evaluate(profile(1, "CSE", 2027, "8.00", 1), d);
        assertThat(r.eligible()).isFalse();
        assertThat(r.reasons()).singleElement().asString().contains("backlog");
    }

    @Test
    void branchNotInTheListIsRejectedButMatchingIgnoresCaseAndSpaces() {
        assertThat(EligibilityEvaluator.evaluate(profile(1, "ME", 2027, "8.00", 0), d).eligible()).isFalse();
        assertThat(EligibilityEvaluator.evaluate(profile(1, " it ", 2027, "8.00", 0), d).eligible()).isTrue();
    }

    @Test
    void emptyBranchListMeansEveryBranchIsEligible() {
        PlacementDrive open = drive(2, 1, DriveStatus.OPEN);
        open.getBranches().clear();
        assertThat(EligibilityEvaluator.evaluate(profile(1, "Mechanical", 2027, "8.00", 0), open).eligible()).isTrue();
    }

    @Test
    void criteriaThatAreNotSetAreNotChecked() {
        PlacementDrive loose = new PlacementDrive();
        loose.applyDetails(1L, "Role", null, null, null, (short) 2027, null, null, Set.of(), PlacementFixtures.NOW);
        assertThat(EligibilityEvaluator.evaluate(profile(1, null, 2027, null, null), loose).eligible()).isTrue();
    }

    @Test
    void everyFailedCriterionIsReportedTogether() {
        EligibilityResult r = EligibilityEvaluator.evaluate(profile(1, "ME", 2026, "5.00", 3), d);
        assertThat(r.reasons()).hasSize(4);

    }
}
