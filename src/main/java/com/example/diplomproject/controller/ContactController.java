package com.example.diplomproject.controller;

import com.example.diplomproject.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
public class ContactController {

    private final EmailService emailService;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Autowired
    public ContactController(EmailService emailService) {
        this.emailService = emailService;
    }

    @GetMapping("/contacts")
    public String showContactsPage(Model model) {
        model.addAttribute("content", "pages/contacts/contacts :: contacts-content");
        return "layouts/main";
    }

    @PostMapping("/send-message")
    public String sendMessage(@RequestParam("name") String name,
                              @RequestParam("email") String email,
                              @RequestParam(value = "phone", required = false) String phone,
                              @RequestParam("message") String message,
                              RedirectAttributes redirectAttributes) {
        // Валидация
        if (name == null || name.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Введите имя");
            return "redirect:/contacts";
        }
        if (email == null || email.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Введите email");
            return "redirect:/contacts";
        }
        if (message == null || message.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Введите сообщение");
            return "redirect:/contacts";
        }

        try {
            String adminText = String.format("Имя: %s\nEmail: %s\nТелефон: %s\nСообщение:\n%s",
                    name, email, (phone != null && !phone.isEmpty()) ? phone : "не указан", message);
            emailService.sendSimpleEmail(adminEmail, "Школа красоты", adminText);

            String clientText = String.format("%s, спасибо! Мы свяжемся с вами.", name);
            emailService.sendSimpleEmail(email, "Заявка принята", clientText);

            redirectAttributes.addFlashAttribute("success", "Сообщение отправлено");
        } catch (Exception e) {
            log.error("Ошибка отправки письма", e);
            redirectAttributes.addFlashAttribute("error", "Не удалось отправить сообщение. Попробуйте позже.");
        }
        return "redirect:/contacts";
    }
}