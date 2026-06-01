package org.medcontrol.dto.request;

import java.util.List;

public class IntakeDetailsRequestDto {
    private List<String> availableStatuses;

    public IntakeDetailsRequestDto() {}

    public List<String> getAvailableStatuses() {
        return availableStatuses;
    }

    public void setAvailableStatuses(List<String> availableStatuses) {
        this.availableStatuses = availableStatuses;
    }
}
