package com.example.diplomproject.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    @Test
    void sendSimpleEmail_shouldSendEmail() {
        emailService.sendSimpleEmail("test@example.com", "Subject", "Text");
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendSimpleEmail_shouldThrowWhenToIsBlank() {
        assertThatThrownBy(() -> emailService.sendSimpleEmail("", "Subj", "Text"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email получателя не может быть пустым");
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }
}