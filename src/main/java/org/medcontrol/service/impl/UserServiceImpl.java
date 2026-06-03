package org.medcontrol.service.impl;
import jakarta.persistence.Cacheable;
import jakarta.transaction.Transactional;
import org.hibernate.validator.internal.util.stereotypes.Lazy;
import org.medcontrol.entity.Profile;
import org.medcontrol.repository.PushSubscriptionRepository;
import org.medcontrol.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.medcontrol.dto.ProfileDto;
import org.medcontrol.dto.request.CreateProfileRequestDto;
import org.medcontrol.dto.request.RegisterRequestDto;
import org.medcontrol.dto.response.UserResponseDto;
import org.medcontrol.entity.User;
import org.medcontrol.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Cacheable
@Service
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);
    private UserRepository userRepository;
    private PushSubscriptionRepository pushRepository;
    private PasswordEncoder passwordEncoder;
    @Lazy
    private ProfileServiceImpl profileService;

    @Autowired
    public UserServiceImpl(
            UserRepository userRepository,
            PushSubscriptionRepository pushRepository,
            PasswordEncoder passwordEncoder,
            ProfileServiceImpl profileService
    ) {
        this.userRepository = userRepository;
        this.pushRepository = pushRepository;
        this.passwordEncoder = passwordEncoder;
        this.profileService = profileService;
    }

    @Override
    public UserResponseDto createUser(RegisterRequestDto userRegisterDto) {
        if (!userRegisterDto.getPassword().equals(userRegisterDto.getConfirmPassword()))
            throw new IllegalArgumentException("Пароль не подтвержден");

        if (userRepository.existsByUsername(userRegisterDto.getUsername()))
            throw new IllegalArgumentException("Пользователь с таким именем уже существует");

        if (
                userRegisterDto.getUsername().isEmpty()
                        || userRegisterDto.getUsername().isBlank()
                        || userRegisterDto.getPassword().isEmpty()
                        || userRegisterDto.getPassword().isBlank()
                        || userRegisterDto.getConfirmPassword().isEmpty()
                        || userRegisterDto.getConfirmPassword().isBlank()
        ) throw new IllegalArgumentException("Все поля должны быть заполнены");

        User user = new User();
        user.setUsername(userRegisterDto.getUsername());
        user.setPassword(passwordEncoder.encode(userRegisterDto.getPassword()));
        userRepository.save(user);

        CreateProfileRequestDto defaultProfileDto = new CreateProfileRequestDto();
        defaultProfileDto.setDefault(true);
        if (user.getId() == null) {
            throw new IllegalStateException("User saved but ID is null");
        }
        defaultProfileDto.setUserId(user.getId().toString());
        defaultProfileDto.setProfileName("Мой профиль");

        String profileId = profileService.createProfileAndGetId(defaultProfileDto);
        Profile createdProfile = profileService.getProfileById(profileId);

        user.getProfiles().add(createdProfile);

        userRepository.save(user);

        UserResponseDto result = new UserResponseDto();
        result.setUsername(user.getUsername());
        result.setId(user.getId().toString());

        return result;
    }

    @Override
    public void updateUsername(String userId, String newUsername) {
        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        user.setUsername(newUsername);
        userRepository.save(user);
    }

    @Override
    public void changePassword(String username, String currentPassword, String newPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Неверный текущий пароль");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        log.info("Пароль успешно изменен для пользователя: {}", username);
    }

    public UserResponseDto getUserWithProfiles(String userId) {
        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));

        UserResponseDto result = new UserResponseDto();
        result.setId(user.getId().toString());
        result.setUsername(user.getUsername());

        List<ProfileDto> profileDtosos = user.getProfiles().stream()
                .map(profile -> {
                    ProfileDto profileDto = new ProfileDto();
                    profileDto.setId(profile.getId().toString());
                    profileDto.setProfileName(profile.getName());
                    profileDto.setDefault(profile.isDefault());
                    return profileDto;
                })
                .collect(Collectors.toList());

        result.setProfiles(profileDtosos);
        return result;
    }

    @Override
    public boolean isUsernameTaken(String username) {
        return userRepository.existsByUsername(username);
    }

    @Override
    public User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
    }

    @Override
    @Transactional
    public void deleteUser(String id, String password) {
        User user = userRepository.findById(UUID.fromString(id))
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("Неверный пароль");
        }

        pushRepository.deleteByUser(user);
        profileService.deleteAllProfiles(user.getId());
        userRepository.delete(user);
        log.info("Пользователь с ID {} успешно удален", id);
    }
}
