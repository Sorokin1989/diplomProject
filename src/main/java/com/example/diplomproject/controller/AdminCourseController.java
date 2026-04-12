package com.example.diplomproject.controller;

import com.example.diplomproject.dto.CourseDto;
import com.example.diplomproject.entity.Category;
import com.example.diplomproject.entity.Certificate;
import com.example.diplomproject.entity.Course;
import com.example.diplomproject.service.CategoryService;
import com.example.diplomproject.service.CertificateService;
import com.example.diplomproject.service.CourseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
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
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequestMapping("/admin/courses")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCourseController {

    private final CourseService courseService;
    private final CategoryService categoryService;
    private final CertificateService certificateService;

    @Autowired
    public AdminCourseController(CourseService courseService,
                                 CategoryService categoryService,
                                 CertificateService certificateService) {
        this.courseService = courseService;
        this.categoryService = categoryService;
        this.certificateService = certificateService;
    }

    @GetMapping
    public String adminList(@RequestParam(required = false) Long categoryId,
                            @RequestParam(required = false) String success,
                            @RequestParam(required = false) String error,
                            Model model) {
        log.info("GET /admin/courses - categoryId={}", categoryId);
        List<Course> courses;
        if (categoryId != null) {
            Category category = categoryService.getCategoryById(categoryId);
            if (category != null) {
                courses = courseService.getCoursesByCategoryForAdmin(category);
                model.addAttribute("selectedCategory", category);
                log.debug("Filtered by category: {}", category.getTitle());
            } else {
                courses = Collections.emptyList();
                log.warn("Category not found: id={}", categoryId);
            }
        } else {
            courses = courseService.getAllCoursesForAdmin();
            log.debug("All courses count: {}", courses.size());
        }
        model.addAttribute("courses", courses);
        model.addAttribute("title", "Управление курсами");
        model.addAttribute("content", "pages/admin/courses/admin-list :: admin-courses-content");

        if (success != null) model.addAttribute("success", success);
        if (error != null) model.addAttribute("error", error);

        return "layouts/main";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        log.info("GET /admin/courses/new");
        model.addAttribute("course", new Course());
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("title", "Создание курса");
        model.addAttribute("content", "pages/admin/courses/form :: course-form");
        return "layouts/main";
    }

    @PostMapping
    public String createCourse(@ModelAttribute Course course,
                               @RequestParam(value = "categoryId", required = false) Long categoryId,
                               @RequestParam(value = "newImages", required = false) MultipartFile[] images,
                               RedirectAttributes redirectAttributes) {
        log.info("POST /admin/courses - create course: title='{}', categoryId={}", course.getTitle(), categoryId);
        if (categoryId == null || categoryId == 0) {
            log.warn("Category not selected");
            redirectAttributes.addAttribute("error", "Пожалуйста, выберите категорию");
            return "redirect:/admin/courses/new";
        }

        Category category = categoryService.getCategoryById(categoryId);
        if (category == null) {
            log.error("Category id={} not found", categoryId);
            redirectAttributes.addAttribute("error", "Выбранная категория не существует");
            return "redirect:/admin/courses/new";
        }
        course.setCategory(category);

        try {
            Course savedCourse = courseService.createNewCourse(course);
            log.info("Course created with id={}", savedCourse.getId());
            if (images != null && images.length > 0 && !images[0].isEmpty()) {
                courseService.addImagesToCourse(savedCourse.getId(), images);
                log.debug("Added {} images to course id={}", images.length, savedCourse.getId());
            }
            redirectAttributes.addAttribute("success", "Курс успешно создан");
            return "redirect:/admin/courses";
        } catch (Exception e) {
            log.error("Error creating course: {}", e.getMessage(), e);
            redirectAttributes.addAttribute("error", "Ошибка создания: " + e.getMessage());
            return "redirect:/admin/courses/new";
        }
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        log.info("GET /admin/courses/edit/{}", id);
        Course course = courseService.getCourseEntityById(id);
        model.addAttribute("course", course);
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("title", "Редактирование курса");
        model.addAttribute("content", "pages/admin/courses/form :: course-form");
        return "layouts/main";
    }

    @PostMapping("/{id}")
    public String updateCourse(@PathVariable Long id,
                               @ModelAttribute("course") Course course,
                               @RequestParam("categoryId") Long categoryId,
                               @RequestParam(value = "newImages", required = false) MultipartFile[] newImages,
                               RedirectAttributes redirectAttributes) {
        log.info("POST /admin/courses/{} - update, title='{}', categoryId={}", id, course.getTitle(), categoryId);
        try {
            Course existing = courseService.getCourseEntityById(id);
            if (existing == null) {
                log.error("Course id={} not found for update", id);
                redirectAttributes.addAttribute("error", "Курс не найден");
                return "redirect:/admin/courses";
            }

            if (categoryId == null || categoryId == 0) {
                log.warn("Category not selected for update");
                redirectAttributes.addAttribute("error", "Пожалуйста, выберите категорию");
                return "redirect:/admin/courses/edit/" + id;
            }
            Category category = categoryService.getCategoryById(categoryId);
            if (category == null) {
                log.error("Category id={} not found", categoryId);
                redirectAttributes.addAttribute("error", "Выбранная категория не существует");
                return "redirect:/admin/courses/edit/" + id;
            }
            course.setCategory(category);

            courseService.updateCourse(course, id);
            log.info("Course id={} updated", id);

            if (newImages != null && newImages.length > 0) {
                courseService.addImagesToCourse(id, newImages);
                log.debug("Added {} new images to course id={}", newImages.length, id);
            }

            redirectAttributes.addAttribute("success", "Курс обновлён");
            return "redirect:/admin/courses";
        } catch (Exception e) {
            log.error("Error updating course id={}: {}", id, e.getMessage(), e);
            redirectAttributes.addAttribute("error", "Ошибка обновления: " + e.getMessage());
            return "redirect:/admin/courses/edit/" + id;
        }
    }

    @PostMapping("/delete/{id}")
    public String deleteCourse(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        log.info("POST /admin/courses/delete/{}", id);
        try {
            Course course = courseService.getCourseEntityById(id);
            if (course == null) {
                log.warn("Course id={} not found for deletion", id);
                redirectAttributes.addAttribute("error", "Курс не найден");
                return "redirect:/admin/courses";
            }

            List<Certificate> active = certificateService.getActiveCertificatesByCourse(course);
            if (!active.isEmpty()) {
                log.warn("Cannot delete course id={}, active certificates: {}", id, active.size());
                redirectAttributes.addAttribute("error",
                        "Невозможно удалить курс. На него выдано " + active.size() + " активных сертификат(ов). Сначала отзовите их.");
                return "redirect:/admin/courses";
            }

            certificateService.deleteRevokedCertificatesForCourse(course);

            // Сохраняем пути к изображениям до удаления курса (если нужно)
//            List<String> imagePaths = course.getImages().stream()
//                    .map(img -> img.getFilePath())
//                    .collect(Collectors.toList());

            // Сначала удаляем курс из БД
            courseService.deleteCourseById(id);
            log.info("Course id={} ('{}') deleted", id, course.getTitle());

            // Только после успешного удаления из БД удаляем физические файлы
//            for (String filePath : imagePaths) {
//                try {
//                    Path fullPath = Paths.get(System.getProperty("user.dir"), filePath);
//                    Files.deleteIfExists(fullPath);
//                    log.debug("Deleted image file: {}", fullPath);
//                } catch (Exception e) {
//                    log.warn("Failed to delete image file: {} - {}", filePath, e.getMessage());
//                }
//            }

            redirectAttributes.addAttribute("success", "Курс \"" + course.getTitle() + "\" успешно удалён");

        } catch (DataIntegrityViolationException e) {
            log.error("Integrity violation while deleting course id={}: {}", id, e.getMessage());
            redirectAttributes.addAttribute("error",
                    "Невозможно удалить курс, так как он добавлен в корзины пользователей. Сначала удалите его из корзин.");
        } catch (Exception e) {
            log.error("Error deleting course id={}: {}", id, e.getMessage(), e);
            redirectAttributes.addAttribute("error", "Ошибка удаления: " + e.getMessage());
        }
        return "redirect:/admin/courses";
    }
    @PostMapping("/{id}/images/{imageId}/main")
    public String setMainImage(@PathVariable Long id,
                               @PathVariable Long imageId,
                               RedirectAttributes redirectAttributes) {
        log.info("POST /admin/courses/{}/images/{}/main", id, imageId);
        try {
            Course course = courseService.getCourseEntityById(id);
            if (course == null) {
                log.warn("Course id={} not found for setting main image", id);
                redirectAttributes.addAttribute("error", "Курс не найден");
                return "redirect:/admin/courses";
            }

            boolean belongs = course.getImages().stream().anyMatch(img -> img.getId().equals(imageId));
            if (!belongs) {
                log.error("Image id={} does not belong to course id={}", imageId, id);
                redirectAttributes.addAttribute("error", "Изображение не принадлежит данному курсу");
                return "redirect:/admin/courses/edit/" + id;
            }

            courseService.setMainImage(imageId);
            log.info("Image id={} set as main for course id={}", imageId, id);
            redirectAttributes.addAttribute("success", "Главное изображение обновлено");
        } catch (Exception e) {
            log.error("Error setting main image id={} for course id={}: {}", imageId, id, e.getMessage(), e);
            redirectAttributes.addAttribute("error", "Не удалось установить главное изображение: " + e.getMessage());
        }
        return "redirect:/admin/courses/edit/" + id;
    }


    @GetMapping("/{id}/materials")
    public String showMaterialsForm(@PathVariable Long id, Model model) {
        CourseDto courseDto = courseService.getCourseDtoById(id);
        model.addAttribute("course", courseDto);
        model.addAttribute("title", "Материалы курса");
        model.addAttribute("content", "pages/admin/courses/materials :: materials-form");
        return "layouts/main";
    }

    @PostMapping("/{id}/materials")
    public String uploadMaterials(@PathVariable Long id,
                                  @RequestParam("file") MultipartFile file,
                                  RedirectAttributes redirectAttributes) {
        try {
            courseService.uploadCourseMaterials(id, file);
            redirectAttributes.addFlashAttribute("success", "Материалы успешно загружены");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка загрузки: " + e.getMessage());
        }
        // Исправленный редирект
        return "redirect:/admin/courses/" + id + "/materials";
    }

    @PostMapping("/{id}/materials/delete")
    public String deleteMaterials(@PathVariable Long id,
                                  RedirectAttributes redirectAttributes) {
        try {
            courseService.deleteCourseMaterials(id);
            redirectAttributes.addFlashAttribute("success", "Материалы удалены");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка удаления: " + e.getMessage());
        }
        return "redirect:/admin/courses/" + id + "/materials";
    }
}