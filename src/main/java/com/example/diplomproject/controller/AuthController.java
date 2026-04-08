package com.example.diplomproject.controller;

import com.example.diplomproject.dto.CategoryDto;
import com.example.diplomproject.entity.Category;
import com.example.diplomproject.mapper.CategoryMapper;
import com.example.diplomproject.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.stream.Collectors;

@Controller
public class AuthController {

    private final CategoryService categoryService;
    private final CategoryMapper categoryMapper;

    @Autowired
    public AuthController(CategoryService categoryService, CategoryMapper categoryMapper) {
        this.categoryService = categoryService;
        this.categoryMapper = categoryMapper;
    }

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
        model.addAttribute("content", "pages/user/login :: login-content");
        return "layouts/main";
    }

    @GetMapping("/")
    public String home(Model model) {
        // Получаем все категории
        List<Category> allCategories = categoryService.getAllCategories();
        // Берём первые 3 (популярные направления)
        List<Category> popularCategories = allCategories.stream().limit(3).toList();
        // Преобразуем в DTO
        List<CategoryDto> categoryDtos = popularCategories.stream()
                .map(categoryMapper::toCategoryDTO)
                .collect(Collectors.toList());

        model.addAttribute("title", "Главная");
        model.addAttribute("categories", categoryDtos);
        model.addAttribute("content", "pages/home/index :: index-content");
        return "layouts/main";
    }
}