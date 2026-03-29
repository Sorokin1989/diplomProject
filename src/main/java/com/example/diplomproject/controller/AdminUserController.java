package com.example.diplomproject.controller;

import com.example.diplomproject.dto.UserDto;
import com.example.diplomproject.entity.User;
import com.example.diplomproject.enums.Role;
import com.example.diplomproject.mapper.UserMapper;
import com.example.diplomproject.repository.UserRepository;
import com.example.diplomproject.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Controller
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    @Autowired
    private UserRepository userRepository;


    private final UserService userService;
    private final UserMapper userMapper;

    @Autowired
    public AdminUserController(UserService userService, UserMapper userMapper) {
        this.userService = userService;
        this.userMapper = userMapper;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public String listUsers(
            @PageableDefault(size = 20, sort = "username", direction = Sort.Direction.ASC)
            Pageable pageable,
            Model model) {
        Page<User> userPage = userService.getUsersByPages(pageable);
        Page<UserDto> userDtoPage = userPage.map(userMapper::toUserDto);

        model.addAttribute("users", userDtoPage);
        model.addAttribute("currentPage", userPage.getNumber() + 1);
        model.addAttribute("totalPages", userPage.getTotalPages());
        model.addAttribute("totalItems", userPage.getTotalElements());
        model.addAttribute("title", "Управление пользователями");
        model.addAttribute("content", "pages/admin/users/list :: admin-users-list-content");
        return "layouts/main";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}/edit")
    public String showAdminEditUserForm(@PathVariable Long id, Model model) {
        User user = userService.getUserById(id);
        UserDto userDto = userMapper.toUserDto(user);
        model.addAttribute("user", userDto);
        model.addAttribute("roles", Role.values());
        model.addAttribute("title", "Редактирование пользователя");
        model.addAttribute("content", "pages/admin/users/edit :: admin-user-edit-content");
        return "layouts/main";
    }


    @PostMapping("/{id}")
    public String updateUserByAdmin(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id,
            @RequestParam String username,
            @RequestParam String email,
            @RequestParam(required = false) String role,
            Model model) {

        if (currentUser == null) {
            return "redirect:/admin/users?error=" + URLEncoder.encode("Пользователь не аутентифицирован", StandardCharsets.UTF_8);
        }
        if (currentUser.getId().equals(id)) {
            return "redirect:/admin/users/" + id + "/edit?error=" + URLEncoder.encode("Вы не можете редактировать свой профиль", StandardCharsets.UTF_8);
        }

        try {
            User user = userService.getUserById(id);

            if (!user.getUsername().equals(username) && userService.existsByUsername(username)) {
                return "redirect:/admin/users/" + id + "/edit?error=" + URLEncoder.encode("Имя пользователя уже занято", StandardCharsets.UTF_8);
            }
            if (!user.getEmail().equals(email) && userService.existsByEmail(email)) {
                return "redirect:/admin/users/" + id + "/edit?error=" + URLEncoder.encode("Email уже используется", StandardCharsets.UTF_8);
            }

            user.setUsername(username);
            user.setEmail(email);
            if (role != null && !role.trim().isEmpty()) {
                user.setRole(Role.valueOf(role));
            }

            userService.updateUser(id, user);

            return "redirect:/admin/users?success=" + URLEncoder.encode("Пользователь успешно обновлён", StandardCharsets.UTF_8);

        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/admin/users/" + id + "/edit?error=" + URLEncoder.encode("Ошибка обновления: " + e.getMessage(), StandardCharsets.UTF_8);
        }
    }


    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/delete")
    public String deleteUser(@AuthenticationPrincipal User currentUser,
                             @PathVariable Long id) {
        if (currentUser == null) {
            return "redirect:/admin/users?error=" + URLEncoder.encode("Пользователь не аутентифицирован", StandardCharsets.UTF_8);
        }
        if (currentUser.getId().equals(id)) {
            return "redirect:/admin/users?error=" + URLEncoder.encode("Нельзя удалить самого себя", StandardCharsets.UTF_8);
        }
        try {
            userService.deleteUser(id);
            return "redirect:/admin/users?success=" + URLEncoder.encode("Пользователь удалён", StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/admin/users?error=" + URLEncoder.encode("Ошибка удаления: " + e.getMessage(), StandardCharsets.UTF_8);
        }
    }
}