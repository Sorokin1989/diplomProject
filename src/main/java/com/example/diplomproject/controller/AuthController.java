package com.example.diplomproject.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    /**
     * Отображение страницы входа.
     * @param error параметр ошибки, если вход не удался
     * @param logout параметр выхода, если пользователь только что вышел
     * @param model модель для передачи сообщений в шаблон
     * @return имя шаблона страницы входа
     */
    @GetMapping("/login")
    public String login(@RequestParam(value = "error", required = false) String error,
                        @RequestParam(value = "logout", required = false) String logout,
                        Model model) {
        if (error != null) {
            model.addAttribute("error", "Неверное имя пользователя или пароль");
        }
        if (logout != null) {
            model.addAttribute("message", "Вы успешно вышли из системы");
        }
        return "pages/user/login";
    }

    /**
     * Главная страница после входа.
     */
    @GetMapping("/")
    public String home() {
        return "pages/index";
    }
}