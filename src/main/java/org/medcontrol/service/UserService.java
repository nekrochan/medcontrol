package org.medcontrol.service;

import org.medcontrol.dto.request.RegisterRequestDto;
import org.medcontrol.dto.response.UserResponseDto;
import org.medcontrol.entity.User;

public interface UserService {
    UserResponseDto createUser(RegisterRequestDto userRegisterDto);

    void updateUsername(String userId, String newUsername);

    void changePassword(String username, String currentPassword, String newPassword);

    boolean isUsernameTaken(String username);

    User getUser(String username);

    void deleteUser(String id, String password);
}
