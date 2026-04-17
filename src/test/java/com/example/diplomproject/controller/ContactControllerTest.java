package com.example.diplomproject.controller;

import com.example.diplomproject.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ContactControllerTest {

    @Mock
    private EmailService emailService;

    @InjectMocks
    private ContactController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        ReflectionTestUtils.setField(controller, "adminEmail", "admin@school.com");
    }

    @Test
    void showContactsPage_shouldReturnContactsPage() throws Exception {
        mockMvc.perform(get("/contacts"))
                .andExpect(status().isOk())
                .andExpect(view().name("layouts/main"))
                .andExpect(model().attribute("content", "pages/contacts/contacts :: contacts-content"));
    }

    @Test
    void sendMessage_success() throws Exception {
        doNothing().when(emailService).sendSimpleEmail(anyString(), anyString(), anyString());

        mockMvc.perform(post("/send-message")
                        .param("name", "Иван")
                        .param("email", "ivan@example.com")
                        .param("phone", "+79991234567")
                        .param("message", "Хочу записаться на курс"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/contacts"))
                .andExpect(flash().attribute("success", "Сообщение отправлено"));

        verify(emailService, times(2)).sendSimpleEmail(anyString(), anyString(), anyString());
    }

    @Test
    void sendMessage_withoutPhone_success() throws Exception {
        doNothing().when(emailService).sendSimpleEmail(anyString(), anyString(), anyString());

        mockMvc.perform(post("/send-message")
                        .param("name", "Петр")
                        .param("email", "petr@example.com")
                        .param("message", "Есть вопрос"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/contacts"))
                .andExpect(flash().attribute("success", "Сообщение отправлено"));

        verify(emailService, times(2)).sendSimpleEmail(anyString(), anyString(), anyString());
    }

    @Test
    void sendMessage_emptyName_shouldReturnError() throws Exception {
        mockMvc.perform(post("/send-message")
                        .param("name", "")
                        .param("email", "ivan@example.com")
                        .param("message", "Сообщение"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/contacts"))
                .andExpect(flash().attribute("error", "Введите имя"));

        verify(emailService, never()).sendSimpleEmail(anyString(), anyString(), anyString());
    }

    @Test
    void sendMessage_emptyEmail_shouldReturnError() throws Exception {
        mockMvc.perform(post("/send-message")
                        .param("name", "Иван")
                        .param("email", "")
                        .param("message", "Сообщение"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("error", "Введите email"));
    }

    @Test
    void sendMessage_emptyMessage_shouldReturnError() throws Exception {
        mockMvc.perform(post("/send-message")
                        .param("name", "Иван")
                        .param("email", "ivan@example.com")
                        .param("message", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("error", "Введите сообщение"));
    }

    @Test
    void sendMessage_emailServiceThrowsException_shouldReturnError() throws Exception {
        doThrow(new RuntimeException("SMTP error")).when(emailService).sendSimpleEmail(anyString(), anyString(), anyString());

        mockMvc.perform(post("/send-message")
                        .param("name", "Иван")
                        .param("email", "ivan@example.com")
                        .param("message", "Сообщение"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/contacts"))
                .andExpect(flash().attribute("error", "Не удалось отправить сообщение. Попробуйте позже."));

        verify(emailService, times(1)).sendSimpleEmail(anyString(), anyString(), anyString());
    }
}