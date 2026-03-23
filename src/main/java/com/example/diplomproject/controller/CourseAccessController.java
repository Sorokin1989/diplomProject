package com.example.diplomproject.controller;

import com.example.diplomproject.entity.Course;
import com.example.diplomproject.entity.User;
import com.example.diplomproject.service.CourseAccessService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/my-courses")
public class CourseAccessController {

    private final CourseAccessService courseAccessService;

    public CourseAccessController(CourseAccessService courseAccessService) {
        this.courseAccessService = courseAccessService;
    }

    @GetMapping
    public String showMyCourses(@AuthenticationPrincipal User currentUser, Model model) {
        List<Course> courses = courseAccessService.getCoursesByUser(currentUser);
        model.addAttribute("courses", courses);
        return "pages/user/my-courses";   // шаблон /templates/pages/my-courses.html
    }
}