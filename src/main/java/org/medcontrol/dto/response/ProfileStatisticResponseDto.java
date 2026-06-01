package org.medcontrol.dto.response;

import java.io.Serializable;

public class ProfileStatisticResponseDto implements Serializable {
    private String profileId;
    private double takenPart;

    public ProfileStatisticResponseDto() {}

    public double getTakenPart() {
        return takenPart;
    }

    public void setTakenPart(double takenPart) {
        this.takenPart = takenPart;
    }

    public String getProfileId() {
        return profileId;
    }

    public void setProfileId(String profileId) {
        this.profileId = profileId;
    }
}
