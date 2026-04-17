package com.example.diplomproject.controller;

import com.example.diplomproject.entity.User;
import com.example.diplomproject.enums.Role;
import com.example.diplomproject.mapper.UserMapper;
import com.example.diplomproject.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AdminUserControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private AdminUserController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        User admin = new User();
        admin.setId(2L);
        admin.setRole(Role.ADMIN);
        admin.setUsername("admin");
        // Устанавливаем в SecurityContextHolder, чтобы резолвер мог подхватить
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(admin, null, admin.getAuthorities())
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(
                        new PageableHandlerMethodArgumentResolver(),
                        new AuthenticationPrincipalArgumentResolver() // <-- добавить
                )
                .build();
    }

    // ---------- GET /admin/users ----------
    @Test
    void listUsers_shouldReturnViewWithPagedUsers() throws Exception {
        Pageable pageable = PageRequest.of(0, 20);
        User user = new User();
        user.setId(1L);
        user.setRole(Role.USER); // обязательно устанавливаем роль
        Page<User> userPage = new PageImpl<>(List.of(user), pageable, 1);
        when(userService.getUsersByPages(any(Pageable.class))).thenReturn(userPage);
        when(userMapper.toUserDto(any(User.class))).thenReturn(new com.example.diplomproject.dto.UserDto());

        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isOk())
                .andExpect(view().name("layouts/main"))
                .andExpect(model().attributeExists("users", "currentPage", "totalPages", "totalItems", "title", "content"))
                .andExpect(model().attribute("title", "Управление пользователями"))
                .andExpect(model().attribute("content", "pages/admin/users/list :: admin-users-list-content"));
    }

    // ---------- GET /admin/users/{id}/edit ----------
    @Test
    void showAdminEditUserForm_found() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setRole(Role.USER);
        when(userService.getUserById(1L)).thenReturn(user);
        when(userMapper.toUserDto(any(User.class))).thenReturn(new com.example.diplomproject.dto.UserDto());

        mockMvc.perform(get("/admin/users/1/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("layouts/main"))
                .andExpect(model().attributeExists("user", "roles", "title", "content"))
                .andExpect(model().attribute("title", "Редактирование пользователя"))
                .andExpect(model().attribute("content", "pages/admin/users/edit :: admin-user-edit-content"));
    }

    @Test
    void showAdminEditUserForm_notFound() throws Exception {
        when(userService.getUserById(99L)).thenThrow(new RuntimeException("User not found"));

        mockMvc.perform(get("/admin/users/99/edit"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"))
                .andExpect(flash().attribute("error", "Пользователь не найден"));
    }

    // ---------- POST /admin/users/{id} ----------
    @Test
    void updateUserByAdmin_success() throws Exception {
        User admin = new User();
        admin.setId(2L);
        admin.setRole(Role.ADMIN);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(admin, null, admin.getAuthorities())
        );

        User existingUser = new User();
        existingUser.setId(1L);
        existingUser.setUsername("old");
        existingUser.setEmail("old@example.com");
        existingUser.setRole(Role.USER);
        when(userService.getUserById(1L)).thenReturn(existingUser);
        when(userService.existsByUsername("newuser")).thenReturn(false);
        when(userService.existsByEmail("new@example.com")).thenReturn(false);
        // Предполагаем, что updateUser возвращает обновлённого пользователя (не void)
        when(userService.updateUser(eq(1L), any(User.class))).thenReturn(existingUser);

        mockMvc.perform(post("/admin/users/1")
                        .param("username", "newuser")
                        .param("email", "new@example.com")
                        .param("role", "ADMIN"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"))
                .andExpect(flash().attribute("success", "Пользователь успешно обновлён"));

        verify(userService).updateUser(eq(1L), any(User.class));
        SecurityContextHolder.clearContext();
    }

    @Test
    void updateUserByAdmin_selfEdit_shouldFail() throws Exception {
        User admin = new User();
        admin.setId(1L);
        admin.setRole(Role.ADMIN);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(admin, null, admin.getAuthorities())
        );

        mockMvc.perform(post("/admin/users/1")
                        .param("username", "newuser")
                        .param("email", "new@example.com")
                        .param("role", "ADMIN"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users/1/edit"))
                .andExpect(flash().attribute("error", "Вы не можете редактировать свой профиль"));

        verify(userService, never()).updateUser(anyLong(), any());
        SecurityContextHolder.clearContext();
    }

    @Test
    void updateUserByAdmin_usernameExists() throws Exception {
        User admin = new User();
        admin.setId(2L);
        admin.setRole(Role.ADMIN);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(admin, null, admin.getAuthorities())
        );

        User existingUser = new User();
        existingUser.setId(1L);
        existingUser.setUsername("old");
        existingUser.setEmail("old@example.com");
        existingUser.setRole(Role.USER);
        when(userService.getUserById(1L)).thenReturn(existingUser);
        when(userService.existsByUsername("taken")).thenReturn(true);

        mockMvc.perform(post("/admin/users/1")
                        .param("username", "taken")
                        .param("email", "new@example.com")
                        .param("role", "USER"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users/1/edit"))
                .andExpect(flash().attribute("error", "Имя пользователя уже занято"));

        verify(userService, never()).updateUser(anyLong(), any());
        SecurityContextHolder.clearContext();
    }

    @Test
    void updateUserByAdmin_invalidRole() throws Exception {
        User admin = new User();
        admin.setId(2L);
        admin.setRole(Role.ADMIN);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(admin, null, admin.getAuthorities())
        );

        User existingUser = new User();
        existingUser.setId(1L);
        existingUser.setUsername("old");
        existingUser.setEmail("old@example.com");
        existingUser.setRole(Role.USER);
        when(userService.getUserById(1L)).thenReturn(existingUser);

        mockMvc.perform(post("/admin/users/1")
                        .param("username", "newuser")
                        .param("email", "new@example.com")
                        .param("role", "INVALID_ROLE"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users/1/edit"))
                .andExpect(flash().attribute("error", "Некорректная роль"));

        verify(userService, never()).updateUser(anyLong(), any());
        SecurityContextHolder.clearContext();
    }

    // ---------- POST /admin/users/{id}/delete ----------
    @Test
    void deleteUser_success() throws Exception {
        doNothing().when(userService).deleteUser(1L);

        mockMvc.perform(post("/admin/users/1/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));

        verify(userService).deleteUser(1L);
    }

    @Test
    void deleteUser_selfDelete_shouldFail() throws Exception {
        User admin = new User();
        admin.setId(1L);
        admin.setRole(Role.ADMIN);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(admin, null, admin.getAuthorities())
        );

        mockMvc.perform(post("/admin/users/1/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"))
                .andExpect(flash().attribute("error", "Нельзя удалить самого себя"));

        verify(userService, never()).deleteUser(anyLong());
        SecurityContextHolder.clearContext();
    }

    @Test
    void deleteUser_failure() throws Exception {
        User admin = new User();
        admin.setId(2L);
        admin.setRole(Role.ADMIN);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(admin, null, admin.getAuthorities())
        );

        doThrow(new RuntimeException("Delete error")).when(userService).deleteUser(1L);

        mockMvc.perform(post("/admin/users/1/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"))
                .andExpect(flash().attribute("error", "Ошибка удаления: Delete error"));

        verify(userService).deleteUser(1L);
        SecurityContextHolder.clearContext();
    }
}