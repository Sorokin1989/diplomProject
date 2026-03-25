package com.example.diplomproject.controller;

import com.example.diplomproject.dto.RegistrationDto;
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

@Controller
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;

    @Autowired
    public UserController(UserService userService, UserMapper userMapper) {
        this.userService = userService;
        this.userMapper = userMapper;
    }

    // === Регистрация ===

    @PreAuthorize("isAnonymous()")
    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("registrationDto", new RegistrationDto());
        return "pages/user/register";
    }

    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute("registrationDto") RegistrationDto dto,
                               BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            return "pages/user/register";
        }
        try {
            userService.registerNewUser(dto);
            return "redirect:/login?registered";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "pages/user/register";
        }
    }

    // === Профиль пользователя ===

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/profile")
    public String showProfile(@AuthenticationPrincipal User currentUser, Model model) {
        // Загружаем актуальные данные из БД (на случай, если currentUser – неполный объект)
        User user = userService.getUserById(currentUser.getId());
        UserDto userDto = userMapper.toUserDto(user);
        model.addAttribute("user", userDto);
        return "pages/user/profile";
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/profile/edit")
    public String showEditProfileForm(@AuthenticationPrincipal User currentUser, Model model) {
        User user = userService.getUserById(currentUser.getId());
        UserDto userDto = userMapper.toUserDto(user);
        model.addAttribute("user", userDto);
        return "pages/user/edit-profile";
    }

    @PostMapping("/profile/edit")
    public String updateProfile(@AuthenticationPrincipal User currentUser,
                                @Valid @ModelAttribute UserDto userDto,
                                BindingResult bindingResult,
                                Model model) {
        try {
            if (bindingResult.hasErrors()) {
                model.addAttribute("user", userDto);
                return "pages/user/edit-profile";
            }

            userService.updateUserFromDto(currentUser.getId(), userDto);

            return "redirect:/profile?updated";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("user", userService.getUserById(currentUser.getId()));
            return "pages/user/edit-profile";
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