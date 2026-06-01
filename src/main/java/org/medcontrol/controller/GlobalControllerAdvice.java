package org.medcontrol.controller;

import jakarta.servlet.http.HttpSession;
import org.medcontrol.dto.ProfileDto;
import org.medcontrol.entity.Profile;
import org.medcontrol.entity.User;
import org.medcontrol.service.impl.ProfileServiceImpl;
import org.medcontrol.service.impl.UserServiceImpl;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalControllerAdvice {

    private final UserServiceImpl userService;
    private final ProfileServiceImpl profileService;

    public GlobalControllerAdvice(UserServiceImpl userService, ProfileServiceImpl profileService) {
        this.userService = userService;
        this.profileService = profileService;
    }

    @ModelAttribute("profiles")
    public List<ProfileDto> getProfiles(Principal principal) {
        if (principal == null) {
            return List.of();
        }

        try {
            User user = userService.getUserOrThrow(principal.getName());
            return user.getProfiles().stream()
                    .map(profile -> {
                        ProfileDto dto = new ProfileDto();
                        dto.setProfileName(profile.getName());
                        dto.setId(profile.getId().toString());
                        dto.setDefault(profile.isDefault());
                        return dto;
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return List.of();
        }
    }

    @ModelAttribute("activeProfileName")
    public String getActiveProfileName(Principal principal, HttpSession session) {
        if (principal == null) {
            return null;
        }

        String activeProfileId = session != null ?
                (String) session.getAttribute("activeProfileId") : null;

        if (activeProfileId != null) {
            try {
                Profile profile = profileService.getProfileById(activeProfileId);
                return profile.getName();
            } catch (Exception e) {
                return null;
            }
        }

        return null;
    }
}
