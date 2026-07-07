package com.carrierfraud.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RiskAlertTransferTest {

    private RiskAlert sampleAlert() {
        return new RiskAlert(
                "A1", "Carrier-A", 0.5, "Rule", AlertSeverity.MEDIUM, Department.MEDIATION);
    }

    @Test
    void transferTo_setsNewDepartment() {
        RiskAlert alert = sampleAlert();
        alert.accept("alice");

        alert.transferTo(Department.LEGAL, "bob");

        assertThat(alert.getAssignedDepartment()).isEqualTo(Department.LEGAL);
    }

    @Test
    void transferTo_resetsStatusToUnassigned() {
        RiskAlert alert = sampleAlert();
        alert.accept("alice");

        alert.transferTo(Department.LEGAL, "bob");

        assertThat(alert.getAssignmentStatus()).isEqualTo(AlertAssignmentStatus.UNASSIGNED);
        assertThat(alert.getAssignedTo()).isNull();
    }

    @Test
    void transferTo_recordsTransferMetadata() {
        RiskAlert alert = sampleAlert();

        alert.transferTo(Department.LEGAL, "bob");

        assertThat(alert.getLastTransferBy()).isEqualTo("bob");
        assertThat(alert.getLastTransferFromDept()).isEqualTo("MEDIATION");
        assertThat(alert.getLastTransferAt()).isNotNull();
    }

    @Test
    void transferTo_rejectsResolvedAlert() {
        RiskAlert alert = sampleAlert();
        alert.accept("alice");
        alert.startInvestigation();
        alert.resolve("Done");

        assertThatThrownBy(() -> alert.transferTo(Department.LEGAL, "bob"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot transfer resolved alert");
    }

    @Test
    void transferTo_updatesLastActivityAt() {
        RiskAlert alert = sampleAlert();

        alert.transferTo(Department.LEGAL, "bob");

        assertThat(alert.getLastActivityAt()).isNotNull();
    }
}