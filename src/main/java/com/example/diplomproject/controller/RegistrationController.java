package com.example.diplomproject.controller;

import com.example.diplomproject.dto.RegistrationDto;
import com.example.diplomproject.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/register")
public class RegistrationController {

    @Autowired
    private UserService userService;

    /**
     * Показать форму регистрации
     */
    @GetMapping
    public String showRegistrationForm(Model model) {
        model.addAttribute("registrationDto", new RegistrationDto());
        return "pages/user/register";
    }

    /**
     * Обработка регистрации
     */
    @PostMapping
    public String registerUser(@Valid @ModelAttribute("registrationDto") RegistrationDto dto,
                               BindingResult bindingResult,
                               Model model) {
        if (bindingResult.hasErrors()) {
            return "pages/user/register";
        }
        try {
            userService.registerNewUser(dto);
            return "redirect:/login?registered";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "pages/user/register";
        }
    }
}