package org.medcontrol.dto.response;

public class SchemeStatisticResponseDto {
    private String schemeId;
    private double takenPart;
    private double cancelledPart;

    public SchemeStatisticResponseDto() {}

    public String getSchemeId() {
        return schemeId;
    }

    public void setSchemeId(String schemeId) {
        this.schemeId = schemeId;
    }

    public double getTakenPart() {
        return takenPart;
    }

    public void setTakenPart(double takenPart) {
        this.takenPart = takenPart;
    }

    public double getCancelledPart() {
        return cancelledPart;
    }

    public void setCancelledPart(double cancelledPart) {
        this.cancelledPart = cancelledPart;
    }
}
