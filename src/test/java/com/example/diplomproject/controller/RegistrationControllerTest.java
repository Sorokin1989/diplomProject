package com.example.diplomproject.controller;

import com.example.diplomproject.dto.RegistrationDto;
import com.example.diplomproject.entity.User;
import com.example.diplomproject.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class RegistrationControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private RegistrationController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setValidator(validator)
                .build();
    }

    @Test
    void showRegistrationForm_shouldReturnForm() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("layouts/main"))
                .andExpect(model().attributeExists("registrationDto", "title", "content"))
                .andExpect(model().attribute("title", "Регистрация"))
                .andExpect(model().attribute("content", "pages/user/register :: register-content"));
    }

    @Test
    void registerUser_success() throws Exception {
        // Предполагаем, что registerNewUser возвращает User или RegistrationDto
        User savedUser = new User(); // или RegistrationDto
        when(userService.registerNewUser(any(RegistrationDto.class))).thenReturn(savedUser);

        mockMvc.perform(post("/register")
                        .param("username", "newuser")
                        .param("email", "user@example.com")
                        .param("password", "password123")
                        .param("confirmPassword", "password123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered"));
    }

    @Test
    void registerUser_validationErrors_shouldReturnForm() throws Exception {
        // Не передаём обязательные поля
        mockMvc.perform(post("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("layouts/main"))
                .andExpect(model().attributeExists("registrationDto", "title", "content"))
                .andExpect(model().attribute("title", "Регистрация"))
                .andExpect(model().attribute("content", "pages/user/register :: register-content"));

        verify(userService, never()).registerNewUser(any());
    }

    @Test
    void registerUser_duplicateUsername_shouldReturnFormWithError() throws Exception {
        doThrow(new IllegalArgumentException("Username already exists"))
                .when(userService).registerNewUser(any(RegistrationDto.class));

        mockMvc.perform(post("/register")
                        .param("username", "existing")
                        .param("email", "user@example.com")
                        .param("password", "password123")
                        .param("confirmPassword", "password123"))
                .andExpect(status().isOk())
                .andExpect(view().name("layouts/main"))
                .andExpect(model().attributeExists("error"))
                .andExpect(model().attribute("error", "Username already exists"));
    }
}