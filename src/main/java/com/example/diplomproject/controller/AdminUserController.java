package com.example.diplomproject.controller;

import com.example.diplomproject.entity.User;
import com.example.diplomproject.enums.Role;
import com.example.diplomproject.mapper.UserMapper;
import com.example.diplomproject.service.UserService;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Controller
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserService userService;
    private final UserMapper userMapper;

    @Autowired
    public AdminUserController(UserService userService, UserMapper userMapper) {
        this.userService = userService;
        this.userMapper = userMapper;
    }

    @GetMapping
    public String listUsers(@PageableDefault(size = 20, sort = "username", direction = Sort.Direction.ASC) Pageable pageable,
                            Model model) {
        Page<User> userPage = userService.getUsersByPages(pageable);
        model.addAttribute("users", userPage.map(userMapper::toUserDto));
        model.addAttribute("currentPage", userPage.getNumber() + 1);
        model.addAttribute("totalPages", userPage.getTotalPages());
        model.addAttribute("totalItems", userPage.getTotalElements());
        model.addAttribute("title", "Управление пользователями");
        model.addAttribute("content", "pages/admin/users/list :: admin-users-list-content");
        return "layouts/main";
    }

    @GetMapping("/{id}/edit")
    public String showAdminEditUserForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            User user = userService.getUserById(id);
            model.addAttribute("user", userMapper.toUserDto(user));
            model.addAttribute("roles", Role.values());
            model.addAttribute("title", "Редактирование пользователя");
            model.addAttribute("content", "pages/admin/users/edit :: admin-user-edit-content");
            return "layouts/main";
        } catch (Exception e) {
            log.error("Пользователь с id={} не найден", id);
            redirectAttributes.addFlashAttribute("error", "Пользователь не найден");
            return "redirect:/admin/users";
        }
    }

    @PostMapping("/{id}")
    public String updateUserByAdmin(@AuthenticationPrincipal User currentUser,
                                    @PathVariable Long id,
                                    @RequestParam String username,
                                    @RequestParam String email,
                                    @RequestParam(required = false) String role,
                                    RedirectAttributes redirectAttributes) {
        if (currentUser.getId().equals(id)) {
            redirectAttributes.addFlashAttribute("error", "Вы не можете редактировать свой профиль");
            return "redirect:/admin/users/" + id + "/edit";
        }

        try {
            User user = userService.getUserById(id);
            if (user == null) {
                redirectAttributes.addFlashAttribute("error", "Пользователь не найден");
                return "redirect:/admin/users";
            }

            if (!user.getUsername().equals(username) && userService.existsByUsername(username)) {
                redirectAttributes.addFlashAttribute("error", "Имя пользователя уже занято");
                return "redirect:/admin/users/" + id + "/edit";
            }
            if (!user.getEmail().equals(email) && userService.existsByEmail(email)) {
                redirectAttributes.addFlashAttribute("error", "Email уже используется");
                return "redirect:/admin/users/" + id + "/edit";
            }

            user.setUsername(username);
            user.setEmail(email);
            if (role != null && !role.trim().isEmpty()) {
                try {
                    user.setRole(Role.valueOf(role));
                } catch (IllegalArgumentException e) {
                    redirectAttributes.addFlashAttribute("error", "Некорректная роль");
                    return "redirect:/admin/users/" + id + "/edit";
                }
            }

            userService.updateUser(id, user);
            redirectAttributes.addFlashAttribute("success", "Пользователь успешно обновлён");
            return "redirect:/admin/users";

        } catch (Exception e) {
            log.error("Ошибка обновления пользователя {}", id, e);
            redirectAttributes.addFlashAttribute("error", "Ошибка обновления: " + e.getMessage());
            return "redirect:/admin/users/" + id + "/edit";
        }
    }

    @PostMapping("/{id}/delete")
    public String deleteUser(@AuthenticationPrincipal User currentUser,
                             @PathVariable Long id,
                             RedirectAttributes redirectAttributes) {
        if (currentUser.getId().equals(id)) {
            redirectAttributes.addFlashAttribute("error", "Нельзя удалить самого себя");
            return "redirect:/admin/users";
        }
        try {
            userService.deleteUser(id);
            redirectAttributes.addFlashAttribute("success", "Пользователь удалён");
        } catch (Exception e) {
            log.error("Ошибка удаления пользователя {}", id, e);
            redirectAttributes.addFlashAttribute("error", "Ошибка удаления: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    private String encode(String message) {
        return URLEncoder.encode(message, StandardCharsets.UTF_8);
    }
}