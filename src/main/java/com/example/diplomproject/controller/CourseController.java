package com.example.diplomproject.controller;

import com.example.diplomproject.entity.Category;
import com.example.diplomproject.entity.Course;
import com.example.diplomproject.service.CategoryService;
import com.example.diplomproject.service.CourseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/courses")
public class CourseController {

    private final CourseService courseService;
    private final CategoryService categoryService;

    @Autowired
    public CourseController(CourseService courseService, CategoryService categoryService) {
        this.courseService = courseService;
        this.categoryService = categoryService;
    }

    // === Публичные методы ===

    /**
     * Список всех курсов (доступен всем)
     */
    @GetMapping
    public String listCourses(Model model) {
        List<Course> courses = courseService.getAllCourses();
        model.addAttribute("courses", courses);
        return "pages/courses/list";
    }

    /**
     * Детальная страница курса
     */
    @GetMapping("/{id}")
    public String viewCourse(@PathVariable Long id, Model model) {
        Course course = courseService.getCourseById(id);
        model.addAttribute("course", course);
        return "pages/courses/detail";
    }

    /**
     * Поиск курсов по названию (частичное совпадение)
     */
    @GetMapping("/search")
    public String searchCourses(@RequestParam(required = false) String title, Model model) {
        List<Course> courses = courseService.searchCoursesByTitle(title);
        model.addAttribute("courses", courses);
        model.addAttribute("searchTitle", title);
        return "pages/courses/list";
    }

    // === Административные методы (только для роли ADMIN) ===

    /**
     * Форма создания нового курса
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("course", new Course());
        List<Category> categories = categoryService.getAllCategories();
        model.addAttribute("categories", categories);
        return "pages/admin/courses/form";
    }

    /**
     * Обработка создания курса
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public String createCourse(@ModelAttribute("course") Course course) {
        courseService.createNewCourse(course);
        return "redirect:/courses";
    }

    /**
     * Форма редактирования курса
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Course course = courseService.getCourseById(id);
        model.addAttribute("course", course);
        List<Category> categories = categoryService.getAllCategories();
        model.addAttribute("categories", categories);
        return "pages/admin/courses/form";
    }

    /**
     * Обработка обновления курса
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}")
    public String updateCourse(@PathVariable Long id, @ModelAttribute("course") Course updatedCourse) {
        courseService.updateCourse(updatedCourse, id);
        return "redirect:/courses";
    }

    /**
     * Удаление курса (POST‑запрос для безопасности)
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/delete/{id}")
    public String deleteCourse(@PathVariable Long id) {
        courseService.deleteCourseByID(id);
        return "redirect:/courses";
    }
}