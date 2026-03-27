package com.example.diplomproject.controller;

import com.example.diplomproject.entity.Category;
import com.example.diplomproject.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/categories")
public class CategoryController {

    private final CategoryService categoryService;

    @Autowired
    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    // Публичный список категорий (доступен всем)
    @GetMapping
    public String listCategories(Model model) {
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("title", "Категории курсов");
        model.addAttribute("content", "/pages/categories/categories :: categories-content"); // путь к фрагменту
        return "layouts/main";
    }

    // Форма создания категории (только для админов)
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("category", new Category());
        return "pages/categories/form";
    }

    // Сохранение новой категории (только для админов)
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public String createCategory(@ModelAttribute("category") Category category) {
        categoryService.addNewCategory(category);
        return "redirect:/categories";
    }

    // Форма редактирования категории (только для админов)
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Category category = categoryService.getCategoryById(id);
        model.addAttribute("category", category);
        return "pages/categories/form";
    }

    // Обновление категории (только для админов)
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}")
    public String updateCategory(@PathVariable Long id, @ModelAttribute("category") Category category) {
        categoryService.updateCategory(id, category);
        return "redirect:/categories";
    }

    // Удаление категории (только для админов)
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/delete/{id}")
    public String deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategoryByID(id);
        return "redirect:/categories";
    }
}