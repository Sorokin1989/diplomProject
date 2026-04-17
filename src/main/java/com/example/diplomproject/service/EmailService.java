package com.example.diplomproject.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {


    private final JavaMailSender mailSender;
    @Value("${app.admin.email}")
    private String fromEmail;

    @Autowired
    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendSimpleEmail(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();

        if (to == null || to.isBlank()) {
            throw new IllegalArgumentException("Email получателя не может быть пустым");
        }
        message.setTo(to);
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("Тема письма не может быть пустой");
        }
        message.setSubject(subject);
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Текст письма не может быть пустым");
        }
        message.setText(text);
        message.setFrom(fromEmail);
        mailSender.send(message);
    }
}