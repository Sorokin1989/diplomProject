package com.example.diplomproject.controller;

import com.example.diplomproject.dto.UserDto;
import com.example.diplomproject.entity.User;
import com.example.diplomproject.enums.Role;
import com.example.diplomproject.mapper.UserMapper;
import com.example.diplomproject.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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

        System.out.println(">>> showProfile called with user: " + currentUser);
        User user = userService.findByUsername(currentUser);
        System.out.println(">>> user found: " + user);

//        User user = userService.findByUsername(currentUser);
        UserDto userDto = userMapper.toUserDto(user);
        model.addAttribute("title", "Мой профиль");
        model.addAttribute("user", userDto);
        model.addAttribute("content", "pages/user/profile :: profile-content");
        return "layouts/main";
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/profile/edit")
    public String showEditProfileForm(@AuthenticationPrincipal(expression = "username") String currentUser, Model model) {
        User user = userService.findByUsername(currentUser);
        UserDto userDto = userMapper.toUserDto(user);
        model.addAttribute("title", "Редактирование профиля");
        model.addAttribute("user", userDto);
        model.addAttribute("content", "pages/user/edit-profile :: edit-profile-content");
        return "layouts/main";
    }

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
            boolean updated = userService.updateUserFromDto(user.getId(), userDto);


            if (updated) {
                redirectAttributes.addFlashAttribute("updated", true);
                return "redirect:/profile";
            }
            else {
                model.addAttribute("message", "Данные не были изменены");
                User freshUser = userService.findByUsername(currentUsername);
                UserDto freshUserDto = userMapper.toUserDto(freshUser);
                model.addAttribute("user", freshUserDto);
                model.addAttribute("title", "Редактирование профиля");
                model.addAttribute("content", "pages/user/edit-profile :: edit-profile-content");
                return "layouts/main";
            }
        } catch (IllegalArgumentException e) {
            User freshUser = userService.findByUsername(currentUsername);
            UserDto freshUserDto = userMapper.toUserDto(freshUser);
            model.addAttribute("error", e.getMessage());
            model.addAttribute("user", freshUserDto);
            model.addAttribute("title", "Редактирование профиля");
            model.addAttribute("content", "pages/user/edit-profile :: edit-profile-content");
            return "layouts/main";
        }
    }

}