package org.medcontrol.service;

import org.medcontrol.dto.request.CreateProfileRequestDto;
import org.medcontrol.entity.Profile;

import java.util.UUID;

public interface ProfileService {
    String createProfileAndGetId(CreateProfileRequestDto dto);

    void deleteProfile(String id);

    void deleteAllProfiles(UUID userId);

    void renameProfile(String id, String newName);

    Profile getProfileById(String profileId);
}
