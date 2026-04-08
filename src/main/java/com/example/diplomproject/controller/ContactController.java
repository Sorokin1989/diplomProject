package com.example.diplomproject.controller;

import com.example.diplomproject.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;


@Controller
public class ContactController {

    private final EmailService emailService;

    @Autowired
    public ContactController(EmailService emailService) {
        this.emailService = emailService;
    }

    @GetMapping("/contacts")
    public String showContactsPage(Model model) {
        model.addAttribute("content", "pages/contacts/contacts :: contacts-content"); // <-- шаблон :: фрагмент
        return "layouts/main";
    }

    // Обработка отправки формы
    @PostMapping("/send-message")
    public String sendMessage(@RequestParam("name") String name,
                              @RequestParam("email") String email,
                              @RequestParam(value = "phone", required = false) String phone,
                              @RequestParam("message") String message) {

        // Отправка письма администратору (замените email на реальный)
        String adminText = String.format("Имя: %s\nEmail: %s\nТелефон: %s\nСообщение:\n%s",
                name, email, (phone != null && !phone.isEmpty()) ? phone : "не указан", message);
        emailService.sendSimpleEmail("admin@ваш-домен.ru", "Заявка с сайта", adminText);

        // Письмо клиенту (опционально)
        String clientText = String.format("%s, спасибо! Мы свяжемся с вами.", name);
        emailService.sendSimpleEmail(email, "Заявка принята", clientText);

        // Редирект с параметром success – сообщение об успехе показываем через GET-параметр
        return "redirect:/contacts?success=true";
    }
}