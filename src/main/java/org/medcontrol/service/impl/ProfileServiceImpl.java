package org.medcontrol.service.impl;
import jakarta.persistence.Cacheable;
import org.medcontrol.entity.*;
import org.medcontrol.entity.enums.IntakeStatus;
import org.medcontrol.service.ProfileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.medcontrol.dto.request.CreateProfileRequestDto;
import org.medcontrol.dto.response.ProfileResponseDto;
import org.medcontrol.dto.response.ProfileStatisticResponseDto;
import org.medcontrol.repository.IntakeRepository;
import org.medcontrol.repository.ProfileRepository;
import org.medcontrol.repository.SchemeRepository;
import org.medcontrol.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Cacheable
@Service
public class ProfileServiceImpl implements ProfileService {

    private static final Logger log = LoggerFactory.getLogger(ProfileServiceImpl.class);
    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final SchemeRepository schemeRepository;
    private final IntakeRepository intakeRepository;
    private final SchemeServiceImpl schemeService;

    @Autowired
    public ProfileServiceImpl(
            ProfileRepository profileRepository,
            UserRepository userRepository,
            SchemeRepository schemeRepository,
            IntakeRepository intakeRepository,
            SchemeServiceImpl schemeService
    ) {
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
        this.schemeRepository = schemeRepository;
        this.intakeRepository = intakeRepository;
        this.schemeService = schemeService;
    }

    @Override
    public String createProfileAndGetId(CreateProfileRequestDto dto) {
        if (!userRepository.existsById(UUID.fromString(dto.getUserId())))
            throw new IllegalArgumentException("Пользователь не найден");
        if (profileRepository
                .existsByNameAndUserId(
                        dto.getProfileName(),
                        UUID.fromString(dto.getUserId())
                )
        )
            throw new RuntimeException("Профиль с таким названием уже существует");

        User user = userRepository.getReferenceById(UUID.fromString(dto.getUserId()));
        Profile profile = new Profile(user, dto.getProfileName(), dto.isDefault());

        profileRepository.save(profile);
        return profile.getId().toString();
    }

    @Override
    public void deleteProfile(String id) {
        Profile profile = profileRepository.findById(UUID.fromString(id))
                .orElseThrow(() -> new IllegalArgumentException("Профиль не найден"));

        if (profile.isDefault()) {
            throw new IllegalArgumentException("Нельзя удалить профиль по умолчанию");
        }

        User user = profile.getUser();
        long defaultProfilesCount = profileRepository.findByUserId(user.getId()).stream()
                .filter(Profile::isDefault)
                .count();

        if (defaultProfilesCount <= 1 && profile.isDefault()) {
            throw new IllegalStateException("Нельзя удалить единственный дефолтный профиль");
        }

        List<Scheme> schemes = schemeRepository.findByProfileId(UUID.fromString(id));
        for (Scheme scheme : schemes) {
            schemeService.deleteScheme(scheme.getId().toString());
        }

        profileRepository.deleteById(UUID.fromString(id));
    }

    @Override
    public void deleteAllProfiles(UUID userId) {
        List<Profile> profiles = profileRepository.findByUserId(userId);
        for (Profile profile : profiles) {
            List<Scheme> schemes = schemeRepository.findByProfileId(UUID.fromString(profile.getId().toString()));
            for (Scheme scheme : schemes) {
                schemeService.deleteScheme(scheme.getId().toString());
            }
            profileRepository.deleteById(UUID.fromString(profile.getId().toString()));
        }
    }

    @Override
    public void renameProfile(String id, String newName) {
        if (!profileRepository.existsById(UUID.fromString(id)))
            throw new RuntimeException("Профиль с таким id не найден");
        Profile profile = profileRepository.findById(UUID.fromString(id))
                .orElseThrow(() -> new IllegalArgumentException("Профиль не найден"));
        if (profileRepository.existsByNameAndUserId(newName, profile.getUser().getId()))
            throw new RuntimeException("Профиль с таким названием уже существует");

        profile.setName(newName);
        profileRepository.save(profile);
    }

    public List<ProfileResponseDto> getProfilesByUser(String userId) {
        UUID userUuid = UUID.fromString(userId);
        List<Profile> profiles = profileRepository.findByUserId(userUuid);

        if (profiles.isEmpty()) {
            throw new IllegalArgumentException("У пользователя нет профилей");
        }

        return profiles.stream()
                .sorted((p1, p2) -> {
                    if (p1.isDefault() && !p2.isDefault()) return -1;
                    if (!p1.isDefault() && p2.isDefault()) return 1;
                    return p1.getName().compareToIgnoreCase(p2.getName());
                })
                .map(this::profileToDto)
                .collect(Collectors.toList());
    }

    @Override
    public Profile getProfileById(String profileId) {
        return profileRepository.findById(UUID.fromString(profileId))
                .orElseThrow(() -> new IllegalArgumentException("Профиль не найден"));

    }

    public ProfileStatisticResponseDto getProfileStatistics(String profileId) {
        UUID profileUuid = UUID.fromString(profileId);

        if (!profileRepository.existsById(profileUuid))
            throw new RuntimeException("Профиль с таким id не найден");

        List<Scheme> schemes = schemeRepository.findByProfileId(profileUuid);
        int totalTaken = 0;
        int totalCancelled = 0;
        double totalTakenPercent = 0.00;

        for (Scheme scheme : schemes) {
            totalCancelled += intakeRepository.countBySchemeIdAndIntakeStatus(scheme.getId(), IntakeStatus.CANCELLED);
            totalTaken += intakeRepository.countBySchemeIdAndIntakeStatus(scheme.getId(), IntakeStatus.TAKEN);
        }
        int total = totalTaken + totalCancelled;

        if (total != 0) {
            totalTakenPercent = totalTaken * 100.00 / total;
        }

        ProfileStatisticResponseDto dto = new ProfileStatisticResponseDto();
        dto.setProfileId(profileId);
        dto.setTakenPart(totalTakenPercent);

        return dto;
    }

    public ProfileResponseDto profileToDto(Profile profile) {
        ProfileResponseDto dto = new ProfileResponseDto();
        dto.setId(profile.getId().toString());
        dto.setProfileName(profile.getName());
        dto.setDefault(profile.isDefault());
        dto.setUserId(profile.getUser().getId().toString());
        return dto;
    }
}
