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

import java.security.Principal;

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
    // === Административные методы ===

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/users")
    public String listUsers(
            @PageableDefault(size = 20, sort = "username", direction = Sort.Direction.ASC)
            Pageable pageable,
            Model model) {
        Page<User> userPage = userService.getUsersByPages(pageable);

        model.addAttribute("users", userPage.map(
                userMapper::toUserDto
        ));
        model.addAttribute("currentPage", userPage.getNumber() +1);
        model.addAttribute("totalPages",userPage.getTotalPages());
        model.addAttribute("totalItems",userPage.getTotalElements());
        return "pages/admin/users/list";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/users/{id}/edit")
    public String showAdminEditUserForm(@PathVariable Long id, Model model) {
        User user = userService.getUserById(id);
        UserDto userDto = userMapper.toUserDto(user);
        model.addAttribute("user", userDto);
        return "pages/admin/users/edit";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/users/{id}")
    public String updateUserByAdmin(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id,
                                    @RequestParam String username,
                                    @RequestParam String email,
                                    @RequestParam(required = false) Role role) {

        if(currentUser.getId().equals(id)){
            throw new IllegalArgumentException("Вы не можете редактировать свой профиль");
        }

        UserDto userDto = new UserDto();
        userDto.setUsername(username);
        userDto.setEmail(email);
        if (null != role) {
            userDto.setRole(role.name());
        }

        userService.updateUserFromDto(id, userDto);
        return "redirect:/admin/users";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/users/{id}/delete")
    public String deleteUser(@AuthenticationPrincipal User currentUser,
                             @PathVariable Long id) {
        if(currentUser.getId().equals(id)){
            throw new IllegalArgumentException("Нельзя удалить самого себя");
        }
        userService.deleteUser(id);
        return "redirect:/admin/users";
    }
}