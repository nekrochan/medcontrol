package org.medcontrol.dto.request;

import jakarta.validation.constraints.NotBlank;

public class CreateProfileRequestDto {
    private String userId;
    private String profileName;
    private boolean isDefault;

    public CreateProfileRequestDto() {
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getProfileName() {
        return profileName;
    }

    public void setProfileName(String profileName) {
        this.profileName = profileName;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean aDefault) {
        isDefault = aDefault;
    }
}
