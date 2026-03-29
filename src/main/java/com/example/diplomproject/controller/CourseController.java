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
        model.addAttribute("courses", courseService.getAllCourses());
        model.addAttribute("title", "Курсы");
        model.addAttribute("content", "pages/courses/courses :: user-courses-content");
        return "layouts/main";
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

}