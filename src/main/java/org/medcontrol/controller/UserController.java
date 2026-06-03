package org.medcontrol.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.medcontrol.dto.ProfileDto;
import org.medcontrol.dto.request.RegisterRequestDto;
import org.medcontrol.dto.response.UserResponseDto;
import org.medcontrol.entity.Profile;
import org.medcontrol.entity.User;
import org.medcontrol.service.impl.UserServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class UserController {
    private static final Logger log = LoggerFactory.getLogger(UserController.class);
    private UserServiceImpl userService;
    private AuthenticationManager authenticationManager;

    @Autowired
    public UserController(UserServiceImpl userService, AuthenticationManager authenticationManager) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
    }

    @GetMapping("/")
    public String root() {
        return "redirect:/login";
    }

    @ModelAttribute("userRegistrationDto")
    public RegisterRequestDto initForm() {
        return new RegisterRequestDto();
    }

    @GetMapping("/register")
    public String register(Model model, Principal principal) {
        if (!model.containsAttribute("userRegistrationDto")) {
            model.addAttribute("userRegistrationDto", new RegisterRequestDto());
        }
        return "register";
    }

    @PostMapping("/register")
    public String doRegister(@Valid RegisterRequestDto registerRequestDto,
                             BindingResult bindingResult,
                             RedirectAttributes redirectAttributes,
                             HttpServletRequest request,
                             HttpServletResponse response,
                             Principal principal) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("username", registerRequestDto.getUsername());
            redirectAttributes.addFlashAttribute(
                    "org.springframework.validation.BindingResult.userRegistrationDto",
                    bindingResult);
            return "redirect:/register";
        }

        if (!registerRequestDto.getPassword().equals(registerRequestDto.getConfirmPassword())) {
            redirectAttributes.addFlashAttribute("username", registerRequestDto.getUsername());
            redirectAttributes.addFlashAttribute("NonConfirmedPassword", true);

            BindingResult passwordBindingResult = new BeanPropertyBindingResult(
                    registerRequestDto, "userRegistrationDto"
            );
            passwordBindingResult.rejectValue(
                    "confirmPassword",
                    "error.password.mismatch",
                    "Passwords do not match"
            );
            redirectAttributes.addFlashAttribute(
                    "org.springframework.validation.BindingResult.userRegistrationDto",
                    passwordBindingResult
            );

            return "redirect:/register";
        }

        try {
            UserResponseDto newUser = this.userService.createUser(registerRequestDto);
            log.info("Пользователь {} успешно создан", registerRequestDto.getUsername());

            User user = userService.getUser(registerRequestDto.getUsername());

            Profile defaultProfile = user.getProfiles().stream()
                    .filter(Profile::isDefault)
                    .findFirst()
                    .orElse(null);

            if (defaultProfile == null) {
                log.error("У пользователя {} нет дефолтного профиля!", registerRequestDto.getUsername());
                redirectAttributes.addFlashAttribute("registrationError",
                        "Ошибка создания профиля. Пожалуйста, обратитесь к администратору.");
                return "redirect:/register";
            }

            log.info("Дефолтный профиль найден: ID={}, Name={}",
                    defaultProfile.getId(), defaultProfile.getName());

            try {
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                registerRequestDto.getUsername(),
                                registerRequestDto.getPassword()
                        );

                Authentication authentication = authenticationManager.authenticate(authToken);
                SecurityContextHolder.getContext().setAuthentication(authentication);

                HttpSession session = request.getSession(true);
                session.setAttribute(
                        HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                        SecurityContextHolder.getContext()
                );

                session.setAttribute("activeProfileId", defaultProfile.getId().toString());
                log.info("Активный профиль установлен в сессию: {}", defaultProfile.getId());
                log.info("ID сессии: {}", session.getId());

                RequestCache requestCache = new HttpSessionRequestCache();
                requestCache.removeRequest(request, response);

                session.removeAttribute("SPRING_SECURITY_SAVED_REQUEST");
                session.removeAttribute("SPRING_SECURITY_LAST_EXCEPTION");
                session.removeAttribute("SPRING_SECURITY_SAVED_REQUEST_KEY");

                log.info("User {} successfully registered and logged in", registerRequestDto.getUsername());

                String activeProfileId = (String) session.getAttribute("activeProfileId");
                log.info("Проверка activeProfileId после установки: {}", activeProfileId);

                if (activeProfileId == null) {
                    log.error("Не удалось установить activeProfileId в сессию!");
                    return "redirect:/user";
                }

                String redirectUrl = "/intakes/date/" + LocalDate.now();
                log.info("Редирект после регистрации на: {}", redirectUrl);
                return "redirect:" + redirectUrl;

            } catch (AuthenticationException e) {
                log.error("Auto-login failed after registration for user: {}",
                        registerRequestDto.getUsername(), e);
                redirectAttributes.addFlashAttribute("registrationSuccess", true);
                return "redirect:/login";
            }

        } catch (RuntimeException e) {
            log.error("Registration failed for user: {}", registerRequestDto.getUsername(), e);

            redirectAttributes.addFlashAttribute("username", registerRequestDto.getUsername());
            redirectAttributes.addFlashAttribute("badCredentials", true);

            bindingResult.rejectValue(
                    "username",
                    "error.user.exists",
                    "This username already exists"
            );
            redirectAttributes.addFlashAttribute(
                    "org.springframework.validation.BindingResult.userRegistrationDto",
                    bindingResult
            );

            return "redirect:/register";
        }
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @PostMapping("/login-error")
    public String onFailedLogin(
            @ModelAttribute(
                    UsernamePasswordAuthenticationFilter.SPRING_SECURITY_FORM_USERNAME_KEY
            ) String username, RedirectAttributes redirectAttributes
    ) {

        redirectAttributes.addFlashAttribute(
                UsernamePasswordAuthenticationFilter.SPRING_SECURITY_FORM_USERNAME_KEY,
                username
        );
        redirectAttributes.addFlashAttribute("badCredentials", true);

        return "redirect:/login";
    }

    @GetMapping("/user")
    public String userPage(Principal principal, Model model, HttpServletRequest request) {

        log.info("Успешная авторзация пользователя : {}", model.getAttribute("username"));
        log.info("Request URI: {}", request.getRequestURI());
        log.info("Context path: {}", request.getContextPath());

        String username = principal.getName();
        User user = userService.getUser(username);

        HttpSession session = request.getSession(false);
        String activeProfileId;
        if (session != null) {
            activeProfileId = (String) session.getAttribute("activeProfileId");
        } else activeProfileId = null;

        if (activeProfileId == null && user.getProfiles() != null && !user.getProfiles().isEmpty()) {
            Profile defaultProfile = user.getProfiles().stream()
                    .filter(Profile::isDefault)
                    .findFirst()
                    .orElse(user.getProfiles().get(0));

            if (session != null && defaultProfile != null) {
                session.setAttribute("activeProfileId", defaultProfile.getId().toString());
                log.info("Восстановлен активный профиль в методе /user: {}", defaultProfile.getId());
            }
        }

        UserResponseDto userResponseDto = new UserResponseDto();
        userResponseDto.setUsername(username);
        userResponseDto.setId(user.getId().toString());
        List<Profile> profiles = user.getProfiles();
        List<ProfileDto> profileDtos = profiles.stream()
                .map(profile -> {
                    ProfileDto dto = new ProfileDto();
                    dto.setProfileName(profile.getName());
                    dto.setId(profile.getId().toString());
                    dto.setDefault(profile.isDefault());
                    return dto;
                })
                .collect(Collectors.toList());

        userResponseDto.setProfiles(profileDtos);
        model.addAttribute("user", userResponseDto);

        return "user-page";
    }

    @PostMapping("/user/username")
    public String updateUsername(@RequestParam String newUsername,
                                 Principal principal,
                                 RedirectAttributes redirectAttributes,
                                 HttpServletRequest request,
                                 HttpServletResponse response) {
        String currentUsername = principal.getName();

        if (newUsername.equals(currentUsername)) {
            redirectAttributes.addFlashAttribute("usernameError",
                    "Новое имя пользователя совпадает с текущим");
            return "redirect:/user";
        }

        if (newUsername == null || newUsername.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("usernameError",
                    "Имя пользователя не может быть пустым");
            return "redirect:/user";
        }

        try {
            User user = userService.getUser(currentUsername);

            if (userService.isUsernameTaken(newUsername)) {
                redirectAttributes.addFlashAttribute("usernameError",
                        "Пользователь с именем '" + newUsername + "' уже существует");
                return "redirect:/user";
            }

            userService.updateUsername(user.getId().toString(), newUsername);
            updateSecurityContext(newUsername, request, response, principal);

            redirectAttributes.addFlashAttribute("usernameSuccess",
                    "Имя пользователя успешно изменено на " + newUsername);
            log.info("Имя пользователя успешно изменено с '{}' на '{}'", currentUsername, newUsername);

        } catch (IllegalArgumentException e) {
            log.error("Ошибка при изменении имени пользователя с '{}' на '{}': {}",
                    currentUsername, newUsername, e.getMessage());
            redirectAttributes.addFlashAttribute("usernameError", e.getMessage());
        } catch (Exception e) {
            log.error("Непредвиденная ошибка при изменении имени пользователя с '{}' to '{}'",
                    currentUsername, newUsername, e);
            redirectAttributes.addFlashAttribute("usernameError",
                    "Произошла ошибка при изменении имени пользователя");
        }

        return "redirect:/user";
    }

    @PostMapping("/user/password")
    public String updatePassword(@RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 Principal principal,
                                 RedirectAttributes redirectAttributes) {
        String username = principal.getName();

        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("passwordError",
                    "Пароли не совпадают");
            return "redirect:/user";
        }

        if (newPassword == null || newPassword.trim().isEmpty() || newPassword.length() < 6) {
            redirectAttributes.addFlashAttribute("passwordError",
                    "Пароль должен содержать минимум 6 символов");
            return "redirect:/user";
        }

        if (newPassword.equals(currentPassword)) {
            redirectAttributes.addFlashAttribute("passwordError",
                    "Новый пароль должен отличаться от текущего");
            return "redirect:/user";
        }

        try {
            userService.changePassword(username, currentPassword, newPassword);
            redirectAttributes.addFlashAttribute("passwordSuccess",
                    "Пароль успешно изменен");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("passwordError", e.getMessage());
        }

        return "redirect:/user";
    }

    private void updateSecurityContext(String newUsername,
                                       HttpServletRequest request,
                                       HttpServletResponse response,
                                       Principal principal) {
        Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();

        if (currentAuth != null && currentAuth.isAuthenticated()) {
            UsernamePasswordAuthenticationToken newAuth =
                    new UsernamePasswordAuthenticationToken(
                            newUsername,
                            currentAuth.getCredentials(),
                            currentAuth.getAuthorities()
                    );
            newAuth.setDetails(currentAuth.getDetails());

            SecurityContextHolder.getContext().setAuthentication(newAuth);

            HttpSession session = request.getSession(false);
            if (session != null) {
                session.setAttribute(
                        HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                        SecurityContextHolder.getContext()
                );
            }

            log.info("Security context updated for new username: {}", newUsername);
        }
    }

    @PostMapping("/user/delete")
    public String deleteUser(@RequestParam String password,
                             Principal principal,
                             RedirectAttributes redirectAttributes,
                             HttpServletRequest request) {
        String username = principal.getName();

        try {
            User user = userService.getUser(username);

            if (user == null) {
                redirectAttributes.addFlashAttribute("deleteError",
                        "Пользователь не найден");
                return "redirect:/user";
            }

            userService.deleteUser(user.getId().toString(), password);

            SecurityContextHolder.clearContext();
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }

            log.info("Пользователь {} успешно удалил свой аккаунт", username);

            redirectAttributes.addFlashAttribute("accountDeleted", true);
            return "redirect:/login";

        } catch (IllegalArgumentException e) {
            log.error("Ошибка при удалении пользователя {}: {}", username, e.getMessage());
            redirectAttributes.addFlashAttribute("deleteError", e.getMessage());
            return "redirect:/user";
        } catch (Exception e) {
            log.error("Непредвиденная ошибка при удалении пользователя {}", username, e);
            redirectAttributes.addFlashAttribute("deleteError",
                    "Произошла ошибка при удалении аккаунта. Попробуйте позже.");
            return "redirect:/user";
        }
    }
}
