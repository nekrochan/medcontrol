package org.medcontrol.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

public class IntakeUpdateRequestDto {
    private String intakeId;
    private LocalDateTime takenAt;
    private String Status;

    public IntakeUpdateRequestDto() {}

    public String getIntakeId() {
        return intakeId;
    }

    public void setIntakeId(String intakeId) {
        this.intakeId = intakeId;
    }

    public LocalDateTime getTakenAt() {
        return takenAt;
    }

    public void setTakenAt(LocalDateTime takenAt) {
        this.takenAt = takenAt;
    }

    public String getStatus() {
        return Status;
    }

    public void setStatus(String status) {
        Status = status;
    }
}
