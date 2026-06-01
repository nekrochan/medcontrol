package org.medcontrol.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

public class IntakeCreateRequestDto {
    private String schemeId;
    private LocalDateTime scheduledAt;
    private LocalDateTime takenAt;
    private String intakeStatus;
    private String medicationName;
    private String dosage;
    private String measure;

    public IntakeCreateRequestDto() {}

    public String getSchemeId() {
        return schemeId;
    }

    public void setSchemeId(String schemeId) {
        this.schemeId = schemeId;
    }

    public LocalDateTime getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(LocalDateTime scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public LocalDateTime getTakenAt() {
        return takenAt;
    }

    public void setTakenAt(LocalDateTime takenAt) {
        this.takenAt = takenAt;
    }

    public String getIntakeStatus() {
        return intakeStatus;
    }

    public void setIntakeStatus(String intakeStatus) {
        this.intakeStatus = intakeStatus;
    }

    public String getMedicationName() {
        return medicationName;
    }

    public void setMedicationName(String medicationName) {
        this.medicationName = medicationName;
    }

    public String getDosage() {
        return dosage;
    }

    public void setDosage(String dosage) {
        this.dosage = dosage;
    }

    public String getMeasure() {
        return measure;
    }

    public void setMeasure(String measure) {
        this.measure = measure;
    }
}
