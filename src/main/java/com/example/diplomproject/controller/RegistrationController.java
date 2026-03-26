package com.example.diplomproject.controller;

import com.example.diplomproject.dto.RegistrationDto;
import com.example.diplomproject.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@PreAuthorize("isAnonymous()")
@Controller
@RequestMapping("/register")
public class RegistrationController {
    private final UserService userService;

    @Autowired
    public RegistrationController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Показать форму регистрации
     */
    @GetMapping
    public String showRegistrationForm(Model model) {
        model.addAttribute("title", "Регистрация");
        model.addAttribute("registrationDto", new RegistrationDto());
        model.addAttribute("content", "pages/user/register :: register-content");
        return "layouts/main";
    }


    /**
     * Обработка регистрации
     */
    @PostMapping
    public String registerUser(@Valid @ModelAttribute("registrationDto") RegistrationDto dto,
                               BindingResult bindingResult,
                               Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("title", "Регистрация");
            model.addAttribute("content", "pages/user/register :: register-content");
            return "layouts/main";
        }
        try {
            userService.registerNewUser(dto);
            return "redirect:/login?registered";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("title", "Регистрация");
            model.addAttribute("content", "pages/user/register :: register-content");
            return "layouts/main";
        }
    }
}