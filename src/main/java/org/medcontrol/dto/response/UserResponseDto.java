package org.medcontrol.dto.response;

import org.medcontrol.dto.ProfileDto;
import org.medcontrol.entity.Profile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class UserResponseDto {
    private String id;
    private String username;
    private List<ProfileDto> profiles = new ArrayList<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public List<ProfileDto> getProfiles() {
        return profiles;
    }

    public void setProfiles(List<ProfileDto> profiles) {
        this.profiles = profiles;
    }


}
