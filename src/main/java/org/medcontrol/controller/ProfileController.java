package org.medcontrol.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.medcontrol.dto.ProfileDto;
import org.medcontrol.dto.request.CreateProfileRequestDto;
import org.medcontrol.dto.response.*;
import org.medcontrol.entity.Profile;
import org.medcontrol.entity.User;
import org.medcontrol.service.impl.ProfileServiceImpl;
import org.medcontrol.service.impl.UserServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/profiles")
public class ProfileController {

    private static final Logger log = LoggerFactory.getLogger(ProfileController.class);
    private final ProfileServiceImpl profileService;
    private final UserServiceImpl userService;

    public ProfileController(ProfileServiceImpl profileService, UserServiceImpl userService) {
        this.profileService = profileService;
        this.userService = userService;
    }

    @GetMapping
    public String getAllProfiles(Principal principal, Model model) {
        String username = principal.getName();
        User user = userService.getUserOrThrow(username);

        List<Profile> profiles = user.getProfiles();
        List<ProfileDto> profileDtos = profiles.stream()
                .map(profile -> {
                    ProfileDto dto = new ProfileDto();
                    dto.setProfileName(profile.getName());
                    dto.setId(profile.getId().toString());
                    return dto;
                })
                .collect(Collectors.toList());

        model.addAttribute("profiles", profileDtos);
        model.addAttribute("username", username);

        return "profiles";
    }

    @PostMapping("/add")
    public String addProfile(
            @Valid @ModelAttribute("profileModel") ProfileDto profileModel,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Principal principal
    ) {
        log.info("Post Request: Создание нового профиля");

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("profileModel", profileModel);
            redirectAttributes.addFlashAttribute(
                    "org.springframework.validation.BindingResult.profileModel",
                    bindingResult
            );
            redirectAttributes.addFlashAttribute(
                    "profileError",
                    "Ошибка валидации названия профиля"
            );
            return "redirect:/user";
        }

        try {
            User user = userService.getUserOrThrow(principal.getName());
            CreateProfileRequestDto dto = new CreateProfileRequestDto();
            dto.setUserId(user.getId().toString());
            dto.setProfileName(profileModel.getProfileName());
            dto.setDefault(false);

            String profileId = profileService.createProfileAndGetId(dto);
            log.info("Создан профиль: {}", profileModel.getProfileName());
            redirectAttributes.addFlashAttribute(
                    "profileSuccess", "Профиль создан"
            );

        } catch (Exception e) {
            log.error("Error creating profile: {}", e.getMessage());
            redirectAttributes.addFlashAttribute(
                    "profileError", "Ошибка создания профиля"
            );
        }

        return "redirect:/user";
    }

    @PostMapping("/{profileId}/rename")
    public String renameProfile(
            @PathVariable String profileId,
            @RequestParam String profileName,
            RedirectAttributes redirectAttributes,
            Principal principal
    ) {
        log.info("Post Request: rename profile {}", profileId);

        if (profileName == null || profileName.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute(
                    "profileError",
                    "Название не может быть пустым"
            );
            return "redirect:/user";
        }

        try {
            profileService.renameProfile(profileId, profileName.trim());
            redirectAttributes.addFlashAttribute(
                    "profileSuccess", "Профиль переименован"
            );
        } catch (Exception e) {
            log.error("Не удалось переименовать профиль: {}", e.getMessage());
            redirectAttributes.addFlashAttribute(
                    "profileError",
                    "Не удалось переименовать профиль"
            );
        }

        return "redirect:/user";
    }

    @PostMapping("/{profileId}/delete")
    public String deleteProfile(
            @PathVariable String profileId,
            RedirectAttributes redirectAttributes,
            Principal principal
    ) {
        log.info("Post Request: Удаление профиля {}", profileId);

        try {
            Profile profile = profileService.getProfileById(profileId);
            ProfileResponseDto profileResponseDto = profileService.profileToDto(profile);

            if (profile.isDefault()) {
                log.warn("Попытка удаления дефолтного профиля: {}", profileId);
                redirectAttributes.addFlashAttribute("profileError",
                        "Невозможно удалить дефолтный профиль");
                return "redirect:/user";
            }

            profileService.deleteProfile(profileId);
            redirectAttributes.addFlashAttribute(
                    "profileSuccess",
                    "Профиль удален"
            );
        } catch (Exception e) {
            log.error("Error deleting profile: {}", e.getMessage());
            redirectAttributes.addFlashAttribute(
                    "profileError", "Не удалось удалить профиль"
            );
        }

        return "redirect:/user";
    }

    @PostMapping("/{profileId}/activate")
    public String activateProfile(
            @PathVariable String profileId,
            HttpSession session,
            Principal principal
    ) {
        log.info("Post Request: Установить активный профиль сессии {}", profileId);

        session.setAttribute("activeProfileId", profileId);

        return "redirect:/intakes";
    }

    @GetMapping("/statistics/{profileId}")
    public String getProfileStatistics(
            @PathVariable String profileId,
            Model model,
            Principal principal
    ) {
        try {
            ProfileStatisticResponseDto statistics = profileService
                    .getProfileStatistics(profileId);
            model.addAttribute("statistics", statistics);
            return "profile-statistics";
        } catch (IllegalArgumentException e) {
            return "redirect:/user";
        }
    }
}
