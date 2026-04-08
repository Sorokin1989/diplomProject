package com.example.diplomproject.controller;

import com.example.diplomproject.dto.CourseDto;
import com.example.diplomproject.entity.Category;
import com.example.diplomproject.entity.Course;
import com.example.diplomproject.entity.Review;
import com.example.diplomproject.mapper.CategoryMapper;
import com.example.diplomproject.mapper.CourseMapper;
import com.example.diplomproject.service.CategoryService;
import com.example.diplomproject.service.CourseService;
import com.example.diplomproject.service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/courses")
public class CourseController {

    private final CourseService courseService;
    private final CourseMapper courseMapper;
    private final CategoryService categoryService;
    private final CategoryMapper categoryMapper;
    private final ReviewService reviewService;

    @Autowired
    public CourseController(CourseService courseService, CourseMapper courseMapper, CategoryService categoryService, CategoryMapper categoryMapper, ReviewService reviewService) {
        this.courseService = courseService;
        this.courseMapper = courseMapper;
        this.categoryService = categoryService;
        this.categoryMapper = categoryMapper;
        this.reviewService = reviewService;
    }

    /**
     * Список курсов с возможностью фильтрации по категории
     */
    @GetMapping
    public String listCourses(@RequestParam(value = "categoryId", required = false) Long categoryId,
                              Model model) {
        List<Course> courses;
        if (categoryId != null) {
            Category category = categoryService.getCategoryById(categoryId);
            if (category != null) {
                model.addAttribute("category", categoryMapper.toCategoryDTO(category));
                courses = courseService.getCoursesByCategoryId(categoryId);
            } else {
                courses = Collections.emptyList();
            }
        } else {
            courses = courseService.getAllCourses();
        }
        List<CourseDto> courseDtos = courses.stream()
                .map(courseMapper::toCourseDto)
                .collect(Collectors.toList());

        model.addAttribute("courses", courseDtos);
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
        List<Review> reviews = reviewService.findByCourseId(id);
        if (course == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Курс не найден");
        }
        CourseDto courseDto = courseMapper.toCourseDto(course);
        model.addAttribute("course", courseDto);
        model.addAttribute("title", courseDto.getTitle());
        model.addAttribute("content", "pages/courses/detail :: detail-content");
        model.addAttribute("reviews", reviews != null ? reviews : Collections.emptyList());
        return "layouts/main";
    }

    /**
     * Поиск курсов по названию (частичное совпадение)
     */
    @GetMapping("/search")
    public String searchCourses(@RequestParam(required = false) String title, Model model) {
        List<Course> courses = courseService.searchCoursesByTitle(title);
        List<CourseDto> courseDtos = courses.stream()
                .map(courseMapper::toCourseDto)
                .collect(Collectors.toList());

        model.addAttribute("courses", courseDtos);
        model.addAttribute("searchTitle", title);
        model.addAttribute("title", title != null && !title.isEmpty()
                ? "Результаты поиска: " + title
                : "Все курсы");
        model.addAttribute("content", "pages/courses/courses :: user-courses-content");
        return "layouts/main";
    }
}