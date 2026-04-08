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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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
    public String adminList(@RequestParam(required = false) String success,
                            @RequestParam(required = false) String error,
                            Model model) {
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("title", "Управление категориями");
        model.addAttribute("content", "pages/admin/categories/admin-list :: admin-categories-content");
        if (success != null) model.addAttribute("success", success);
        if (error != null) model.addAttribute("error", error);
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
                                 @RequestParam(value = "newImages", required = false) MultipartFile[] newImages,
                                 RedirectAttributes redirectAttributes) {
        log.info("Создание категории: title={}", category.getTitle());
        try {
            Category savedCategory = categoryService.addNewCategory(category);
            log.debug("Категория создана с id={}", savedCategory.getId());

            // Фильтруем только непустые файлы
            if (newImages != null && newImages.length > 0) {
                List<MultipartFile> nonEmptyFiles = Arrays.stream(newImages)
                        .filter(f -> f != null && !f.isEmpty())
                        .collect(Collectors.toList());
                if (!nonEmptyFiles.isEmpty()) {
                    log.info("Добавление {} изображений к категории id={}", nonEmptyFiles.size(), savedCategory.getId());
                    categoryService.addImagesToCategory(savedCategory.getId(), nonEmptyFiles.toArray(new MultipartFile[0]));
                } else {
                    log.debug("Нет непустых файлов для загрузки");
                }
            }

            redirectAttributes.addAttribute("success", "Категория успешно создана");
            return "redirect:/admin/categories";
        } catch (Exception e) {
            log.error("Ошибка при создании категории: {}", e.getMessage(), e);
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
                                 @RequestParam(value = "newImages", required = false) MultipartFile[] newImages,
                                 RedirectAttributes redirectAttributes) {
        log.info("Обновление категории id={}, title={}", id, category.getTitle());
        try {
            // Обновляем основные поля категории
            categoryService.updateCategory(id, category);
            log.debug("Основные данные категории id={} обновлены", id);

            // Фильтруем только непустые файлы
            if (newImages != null && newImages.length > 0) {
                List<MultipartFile> nonEmptyFiles = Arrays.stream(newImages)
                        .filter(f -> f != null && !f.isEmpty())
                        .collect(Collectors.toList());
                if (!nonEmptyFiles.isEmpty()) {
                    log.info("Добавление {} новых изображений к категории id={}", nonEmptyFiles.size(), id);
                    categoryService.addImagesToCategory(id, nonEmptyFiles.toArray(new MultipartFile[0]));
                } else {
                    log.debug("Нет непустых файлов для загрузки при обновлении");
                }
            }

            redirectAttributes.addAttribute("success", "Категория обновлена");
            return "redirect:/admin/categories";
        } catch (Exception e) {
            log.error("Ошибка при обновлении категории id={}: {}", id, e.getMessage(), e);
            redirectAttributes.addAttribute("error", "Ошибка обновления: " + e.getMessage());
            return "redirect:/admin/categories/edit/" + id;
        }
    }

    @PostMapping("/delete/{id}")
    public String deleteCategory(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        log.info("Удаление категории id={}", id);
        Category category = categoryService.getCategoryById(id);
        if (category == null) {
            log.warn("Категория с id={} не найдена", id);
            redirectAttributes.addAttribute("error", "Категория не найдена");
            return "redirect:/admin/categories";
        }

        // Проверяем, есть ли курсы
        if (!category.getCourses().isEmpty()) {
            log.warn("Невозможно удалить категорию id={}, есть курсы: {}", id, category.getCourses().size());
            redirectAttributes.addAttribute("error",
                    "Невозможно удалить категорию '" + category.getTitle() + "'. Сначала удалите все курсы в этой категории.");
            return "redirect:/admin/categories";
        }

        // Удаляем категорию (все изображения удалятся в сервисе)
        categoryService.deleteCategoryById(id);
        log.info("Категория id={} успешно удалена", id);
        redirectAttributes.addAttribute("success", "Категория успешно удалена");
        return "redirect:/admin/categories";
    }

    // ========== Управление изображениями ==========

    @PostMapping("/{id}/images/{imageId}/delete")
    public String deleteImage(@PathVariable Long id,
                              @PathVariable Long imageId,
                              RedirectAttributes redirectAttributes) {
        log.info("Удаление изображения imageId={} из категории id={}", imageId, id);
        try {
            categoryService.deleteImage(imageId);
            log.info("Изображение imageId={} удалено", imageId);
            redirectAttributes.addAttribute("success", "Изображение удалено");
        } catch (Exception e) {
            log.error("Ошибка удаления изображения imageId={}: {}", imageId, e.getMessage(), e);
            redirectAttributes.addAttribute("error", "Не удалось удалить изображение: " + e.getMessage());
        }
        return "redirect:/admin/categories/edit/" + id;
    }

    @PostMapping("/{id}/images/{imageId}/main")
    public String setMainImage(@PathVariable Long id,
                               @PathVariable Long imageId,
                               RedirectAttributes redirectAttributes) {
        log.info("Установка главного изображения imageId={} для категории id={}", imageId, id);
        try {
            categoryService.setMainImage(imageId);
            log.info("Изображение imageId={} установлено как главное", imageId);
            redirectAttributes.addAttribute("success", "Главное изображение обновлено");
        } catch (Exception e) {
            log.error("Ошибка установки главного изображения imageId={}: {}", imageId, e.getMessage(), e);
            redirectAttributes.addAttribute("error", "Не удалось установить главное изображение: " + e.getMessage());
        }
        return "redirect:/admin/categories/edit/" + id;
    }
}