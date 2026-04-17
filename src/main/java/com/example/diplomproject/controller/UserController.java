package com.example.diplomproject.controller;

import com.example.diplomproject.dto.UserDto;
import com.example.diplomproject.entity.User;
import com.example.diplomproject.mapper.UserMapper;
import com.example.diplomproject.service.UserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;

    @Autowired
    public UserController(UserService userService, UserMapper userMapper) {
        this.userService = userService;
        this.userMapper = userMapper;
    }

    // === Профиль пользователя ===

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/profile")
    public String showProfile(@AuthenticationPrincipal(expression = "username") String currentUser, Model model) {
        log.debug("showProfile called for user: {}", currentUser);
        User user = userService.findByUsername(currentUser);
        if (user == null) {
            log.warn("User not found: {}", currentUser);
            return "redirect:/login?error=Пользователь не найден";
        }
        UserDto userDto = userMapper.toUserDto(user);
        model.addAttribute("title", "Мой профиль");
        model.addAttribute("user", userDto);
        model.addAttribute("content", "pages/user/profile :: profile-content");
        return "layouts/main";
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/profile/edit")
    public String showEditProfileForm(@AuthenticationPrincipal(expression = "username") String currentUser, Model model) {
        log.debug("showEditProfileForm called for user: {}", currentUser);
        User user = userService.findByUsername(currentUser);
        if (user == null) {
            return "redirect:/login?error=Пользователь не найден";
        }
        UserDto userDto = userMapper.toUserDto(user);
        model.addAttribute("title", "Редактирование профиля");
        model.addAttribute("user", userDto);
        model.addAttribute("content", "pages/user/edit-profile :: edit-profile-content");
        return "layouts/main";
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/profile/edit")
    public String updateProfile(@AuthenticationPrincipal(expression = "username") String currentUsername,
                                @Valid @ModelAttribute UserDto userDto,
                                BindingResult bindingResult,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("title", "Редактирование профиля");
            model.addAttribute("user", userDto);
            model.addAttribute("content", "pages/user/edit-profile :: edit-profile-content");
            return "layouts/main";
        }
        try {
            User user = userService.findByUsername(currentUsername);
            if (user == null) {
                return "redirect:/login?error=Пользователь не найден";
            }
            boolean updated = userService.updateUserFromDto(user.getId(), userDto);
            if (updated) {
                redirectAttributes.addFlashAttribute("success", "Профиль успешно обновлён");
                return "redirect:/profile";
            } else {
                // Данные не изменились – показываем сообщение и остаёмся на странице
                model.addAttribute("message", "Данные не были изменены");
                model.addAttribute("user", userDto);
                model.addAttribute("title", "Редактирование профиля");
                model.addAttribute("content", "pages/user/edit-profile :: edit-profile-content");
                return "layouts/main";
            }
        } catch (IllegalArgumentException e) {
            log.error("Error updating profile for user {}: {}", currentUsername, e.getMessage());
            // Показываем форму с введёнными данными и сообщением об ошибке
            model.addAttribute("error", e.getMessage());
            model.addAttribute("user", userDto); // сохраняем введённые данные
            model.addAttribute("title", "Редактирование профиля");
            model.addAttribute("content", "pages/user/edit-profile :: edit-profile-content");
            return "layouts/main";
        }
    }
}