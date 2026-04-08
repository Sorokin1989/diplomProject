package com.example.diplomproject.controller;

import com.example.diplomproject.dto.CategoryDto;
import com.example.diplomproject.entity.Category;
import com.example.diplomproject.mapper.CategoryMapper;
import com.example.diplomproject.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/categories")
public class CategoryController {

    private final CategoryService categoryService;
    private final CategoryMapper categoryMapper;

    @Autowired
    public CategoryController(CategoryService categoryService, CategoryMapper categoryMapper) {
        this.categoryService = categoryService;
        this.categoryMapper = categoryMapper;
    }

    @GetMapping
    public String listCategories(Model model) {
        List<Category> categories = categoryService.getAllCategories();
        List<CategoryDto> categoryDtos = categories.stream()
                .map(categoryMapper::toCategoryDTO)
                .collect(Collectors.toList());

        model.addAttribute("categories", categoryDtos);
        model.addAttribute("title", "Категории курсов");
        model.addAttribute("content", "pages/categories/categories :: categories-content");
        return "layouts/main";
    }
}