package com.example.diplomproject.controller;

import com.example.diplomproject.dto.UserDto;
import com.example.diplomproject.entity.User;
import com.example.diplomproject.mapper.UserMapper;
import com.example.diplomproject.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setValidator(validator)
                .build();
    }

    private void authenticateUser(String username) {
        User user = new User();
        user.setUsername(username);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(user, null);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void updateProfile_validationErrors_shouldReturnForm() throws Exception {
        authenticateUser("testuser");
        // Убираем стаббинг findByUsername – он не нужен, т.к. валидация не пропустит запрос до него
        mockMvc.perform(post("/profile/edit")
                        .param("username", "")
                        .param("email", "invalid"))
                .andExpect(status().isOk())
                .andExpect(view().name("layouts/main"))
                .andExpect(model().attributeExists("user", "title", "content"))
                .andExpect(model().attribute("title", "Редактирование профиля"));

        verify(userService, never()).updateUserFromDto(anyLong(), any());
        clearAuthentication();
    }

    @Test
    void updateProfile_success() throws Exception {
        authenticateUser("testuser");
        User existingUser = new User();
        existingUser.setId(1L);
        existingUser.setUsername("testuser");
        when(userService.findByUsername("testuser")).thenReturn(existingUser);
        when(userService.updateUserFromDto(eq(1L), any(UserDto.class))).thenReturn(true);

        mockMvc.perform(post("/profile/edit")
                        .param("username", "newuser")
                        .param("email", "new@example.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"))
                .andExpect(flash().attribute("success", "Профиль успешно обновлён"));
        clearAuthentication();
    }

    @Test
    void updateProfile_noChanges() throws Exception {
        authenticateUser("testuser");
        User existingUser = new User();
        existingUser.setId(1L);
        existingUser.setUsername("testuser");
        when(userService.findByUsername("testuser")).thenReturn(existingUser);
        when(userService.updateUserFromDto(eq(1L), any(UserDto.class))).thenReturn(false);

        mockMvc.perform(post("/profile/edit")
                        .param("username", "testuser")
                        .param("email", "same@example.com"))
                .andExpect(status().isOk())
                .andExpect(view().name("layouts/main"))
                .andExpect(model().attribute("message", "Данные не были изменены"));
        clearAuthentication();
    }

    @Test
    void updateProfile_duplicateEmail_shouldShowError() throws Exception {
        authenticateUser("testuser");
        User existingUser = new User();
        existingUser.setId(1L);
        existingUser.setUsername("testuser");
        when(userService.findByUsername("testuser")).thenReturn(existingUser);
        when(userService.updateUserFromDto(eq(1L), any(UserDto.class)))
                .thenThrow(new IllegalArgumentException("Email уже используется"));

        mockMvc.perform(post("/profile/edit")
                        .param("username", "testuser")
                        .param("email", "taken@example.com"))
                .andExpect(status().isOk())
                .andExpect(view().name("layouts/main"))
                .andExpect(model().attributeExists("error"))
                .andExpect(model().attribute("error", "Email уже используется"));
        clearAuthentication();
    }


}