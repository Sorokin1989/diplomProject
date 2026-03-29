package com.example.diplomproject.controller;

import com.example.diplomproject.entity.Category;
import com.example.diplomproject.entity.Course;
import com.example.diplomproject.entity.User;
import com.example.diplomproject.service.CategoryService;
import com.example.diplomproject.service.CourseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
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
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/admin/courses")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCourseController {

    private final CourseService courseService;
    private final CategoryService categoryService;

    @Autowired
    public AdminCourseController(CourseService courseService, CategoryService categoryService) {
        this.courseService = courseService;
        this.categoryService = categoryService;
    }

    // Список курсов (админ) с возможностью фильтрации по категории
    @GetMapping
    public String adminList(@RequestParam(required = false) Long categoryId, Model model) {
        List<Course> courses;
        if (categoryId != null) {
            Category category = categoryService.getCategoryById(categoryId);
            if (category != null) {
                courses = courseService.getByCategory(category);
                model.addAttribute("selectedCategory", category);
            } else {
                courses = Collections.emptyList();
            }
        } else {
            courses = courseService.getAllCourses();
        }
        model.addAttribute("courses", courses);
        model.addAttribute("title", "Управление курсами");
        model.addAttribute("content", "pages/admin/courses/admin-list :: admin-courses-content");
        return "layouts/main";
    }

    // Форма создания курса
    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("course", new Course());
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("title", "Создание курса");
        model.addAttribute("content", "pages/admin/courses/form :: course-form");
        return "layouts/main";
    }

    // Сохранение курса (с картинкой)
    @PostMapping
    public String createCourse(@ModelAttribute Course course,
                               @RequestParam(value = "categoryId", required = false) Long categoryId,
                               @RequestParam("imageFile") MultipartFile imageFile,
                               RedirectAttributes redirectAttributes) {
        // Проверка выбора категории
        if (categoryId == null || categoryId == 0) {
            redirectAttributes.addAttribute("error", "Пожалуйста, выберите категорию");
            return "redirect:/admin/courses/new";
        }

        Category category = categoryService.getCategoryById(categoryId);
        if (category == null) {
            redirectAttributes.addAttribute("error", "Выбранная категория не существует");
            return "redirect:/admin/courses/new";
        }
        course.setCategory(category);

        // Автор остаётся из формы, ничего не меняем

        try {
            if (!imageFile.isEmpty()) {
                String imagePath = saveImage(imageFile, "courses");
                course.setImageUrl(imagePath);
            }
            courseService.createNewCourse(course);
            redirectAttributes.addAttribute("success", "Курс успешно создан");
            return "redirect:/admin/courses";
        } catch (Exception e) {
            redirectAttributes.addAttribute("error", "Ошибка создания: " + e.getMessage());
            return "redirect:/admin/courses/new";
        }
    }
    // Форма редактирования курса
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Course course = courseService.getCourseById(id);
        model.addAttribute("course", course);
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("title", "Редактирование курса");
        model.addAttribute("content", "pages/admin/courses/form :: course-form");
        return "layouts/main";
    }

    // Обновление курса (с картинкой)
    @PostMapping("/{id}")
    public String updateCourse(@PathVariable Long id,
                               @ModelAttribute("course") Course course,
                               @RequestParam("categoryId") Long categoryId,
                               @RequestParam("imageFile") MultipartFile imageFile,
                               RedirectAttributes redirectAttributes) {
        try {
            Course existing = courseService.getCourseById(id);

            // Обработка категории
            if (categoryId == null || categoryId == 0) {
                redirectAttributes.addAttribute("error", "Пожалуйста, выберите категорию");
                return "redirect:/admin/courses/edit/" + id;
            }
            Category category = categoryService.getCategoryById(categoryId);
            if (category == null) {
                redirectAttributes.addAttribute("error", "Выбранная категория не существует");
                return "redirect:/admin/courses/edit/" + id;
            }
            course.setCategory(category);

            // Обработка изображения
            if (!imageFile.isEmpty()) {
                String newImagePath = saveImage(imageFile, "courses");
                if (newImagePath != null) {
                    deleteOldImage(existing.getImageUrl());
                    course.setImageUrl(newImagePath);
                }
            } else {
                // Если новая картинка не загружена, оставляем старую
                course.setImageUrl(existing.getImageUrl());
            }

            // Обновляем курс
            courseService.updateCourse(course, id);
            redirectAttributes.addAttribute("success", "Курс обновлён");
            return "redirect:/admin/courses";

        } catch (Exception e) {
            redirectAttributes.addAttribute("error", "Ошибка обновления: " + e.getMessage());
            return "redirect:/admin/courses/edit/" + id;
        }
    }

    // Удаление курса (удаляем и картинку)
    @PostMapping("/delete/{id}")
    public String deleteCourse(@PathVariable Long id,
                               RedirectAttributes redirectAttributes) {
        try {
            Course course = courseService.getCourseById(id);
            deleteOldImage(course.getImageUrl());
            courseService.deleteCourseByID(id);
            redirectAttributes.addFlashAttribute("success", "Курс удалён");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка удаления: " + e.getMessage());
        }
        return "redirect:/admin/courses";
    }

    // Вспомогательные методы для работы с изображениями
    private String saveImage(MultipartFile file, String subdir) throws IOException {
        String uploadDir = "src/main/resources/static/uploads/" + subdir;
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
        return "/uploads/" + subdir + "/" + fileName;
    }

    private void deleteOldImage(String imageUrl) {
        if (imageUrl != null && !imageUrl.isEmpty()) {
            try {
                Path path = Paths.get("src/main/resources/static" + imageUrl);
                Files.deleteIfExists(path);
            } catch (IOException e) {
                System.err.println("Не удалось удалить файл: " + e.getMessage());
            }
        }
    }

    @PostMapping("/{id}/delete-image")
    public String deleteCourseImage(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Course course = courseService.getCourseById(id);
        if (course == null) {
            redirectAttributes.addAttribute("error", "Курс не найден");
            return "redirect:/admin/courses";
        }
        if (course.getImageUrl() != null && !course.getImageUrl().isEmpty()) {
            deleteOldImage(course.getImageUrl());
        }
        course.setImageUrl(null);
        courseService.updateCourse(course, id);
        redirectAttributes.addAttribute("success", "Изображение удалено");
        return "redirect:/admin/courses/edit/" + id;
    }
}