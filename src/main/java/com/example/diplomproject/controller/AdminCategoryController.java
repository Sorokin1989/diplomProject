package com.example.diplomproject.controller;

import com.example.diplomproject.entity.Category;
import com.example.diplomproject.service.CategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Slf4j
@Controller
@RequestMapping("/admin/categories")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCategoryController {

    private final CategoryService categoryService;

    @Autowired
    public AdminCategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public String adminList(Model model) {
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("title", "Управление категориями");
        model.addAttribute("content", "pages/admin/categories/admin-list :: admin-categories-content");
        return "layouts/main";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("category", new Category());
        model.addAttribute("title", "Создание категории");
        model.addAttribute("content", "pages/admin/categories/form :: category-form");
        return "layouts/main";
    }

    @PostMapping
    public String createCategory(@ModelAttribute("category") Category category,
                                 @RequestParam("imageFile") MultipartFile imageFile,
                                 RedirectAttributes redirectAttributes) {
        try {
            if (!imageFile.isEmpty()) {
                String imagePath = saveImage(imageFile, "categories");
                category.setImageUrl(imagePath);
            }
            categoryService.addNewCategory(category);
            redirectAttributes.addAttribute("success", "Категория успешно создана");
            return "redirect:/admin/categories";
        } catch (Exception e) {
            redirectAttributes.addAttribute("error", "Ошибка создания: " + e.getMessage());
            return "redirect:/admin/categories/new";
        }
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Category category = categoryService.getCategoryById(id);
        model.addAttribute("category", category);
        model.addAttribute("title", "Редактирование категории");
        model.addAttribute("content", "pages/admin/categories/form :: category-form");
        return "layouts/main";
    }

    @PostMapping("/{id}")
    public String updateCategory(@PathVariable Long id,
                                 @ModelAttribute("category") Category category,
                                 @RequestParam("imageFile") MultipartFile imageFile,
                                 RedirectAttributes redirectAttributes) {
        try {
            Category existing = categoryService.getCategoryById(id);

            if (!imageFile.isEmpty()) {
                String newImagePath = saveImage(imageFile, "categories");
                if (newImagePath != null) {
                    deleteOldImage(existing.getImageUrl());
                    category.setImageUrl(newImagePath);
                }
            } else {
                // Если новая картинка не загружена, оставляем старую
                category.setImageUrl(existing.getImageUrl());
            }

            // ✅ ВЫЗОВ updateCategory ВСЕГДА
            categoryService.updateCategory(id, category);
            redirectAttributes.addAttribute("success", "Категория обновлена");
            return "redirect:/admin/categories";

        } catch (Exception e) {
            redirectAttributes.addAttribute("error", "Ошибка обновления: " + e.getMessage());
            return "redirect:/admin/categories/edit/" + id;
        }
    }

    @PostMapping("/delete/{id}")
    public String deleteCategory(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Category category = categoryService.getCategoryById(id);
        if (category == null) {
            redirectAttributes.addAttribute("error", "Категория не найдена");
            return "redirect:/admin/categories";
        }

        // Проверяем, есть ли курсы
        if (!category.getCourses().isEmpty()) {
            redirectAttributes.addAttribute("error",
                    "Невозможно удалить категорию '" + category.getTitle() + "'. Сначала удалите все курсы в этой категории.");
            return "redirect:/admin/categories";
        }

        // Удаляем картинку, если есть
        if (category.getImageUrl() != null && !category.getImageUrl().isEmpty()) {
            deleteImage(category.getImageUrl());
        }


        // Удаляем категорию
        categoryService.deleteCategoryByID(id);
        redirectAttributes.addAttribute("success", "Категория успешно удалена");
        return "redirect:/admin/categories";
    }

    private void deleteImage(String imageUrl) {
        try {
            Path path = Paths.get("src/main/resources/static" + imageUrl);
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("Не удалось удалить файл изображения категории: {}", imageUrl, e);
        }
    }

    private String saveImage(MultipartFile file, String subdir) throws IOException {
        String uploadDir = "uploads/" + subdir;
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String fileName = UUID.randomUUID().toString() + extension;
        Path filePath = uploadPath.resolve(fileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        log.debug("Сохраняю файл в: {}", uploadPath.toAbsolutePath());
        log.debug("Имя файла: {}", fileName);
        return "/uploads/" + subdir + "/" + fileName;
    }

    private void deleteOldImage(String imageUrl) {
        if (imageUrl != null && !imageUrl.isEmpty()) {
            try {
                Path path = Paths.get("src/main/resources/static" + imageUrl);
                Files.deleteIfExists(path);
            } catch (IOException e) {
                log.warn("Не удалось удалить файл: {}", imageUrl, e);
            }
        }
    }

    @PostMapping("/{id}/delete-image")
    public String deleteCategoryImage(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Category category = categoryService.getCategoryById(id);
        if (category == null) {
            redirectAttributes.addAttribute("error", "Категория не найдена");
            return "redirect:/admin/categories";
        }

        // Удаляем файл с диска
        if (category.getImageUrl() != null && !category.getImageUrl().isEmpty()) {
            deleteImage(category.getImageUrl()); // используем существующий метод
        }

        // Очищаем URL в базе
        category.setImageUrl(null);
        categoryService.updateCategory(id, category);

        redirectAttributes.addAttribute("success", "Изображение удалено");
        return "redirect:/admin/categories/edit/" + id;
    }
}