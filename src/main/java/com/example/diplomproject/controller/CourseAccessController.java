package com.example.diplomproject.controller;

import com.example.diplomproject.dto.CourseDto;
import com.example.diplomproject.entity.Course;
import com.example.diplomproject.entity.User;
import com.example.diplomproject.mapper.CourseMapper;
import com.example.diplomproject.service.CourseAccessService;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final CourseMapper courseMapper;

    @Autowired
    public CourseAccessController(CourseAccessService courseAccessService, CourseMapper courseMapper) {
        this.courseAccessService = courseAccessService;
        this.courseMapper = courseMapper;
    }

    @GetMapping
    public String showMyCourses(@AuthenticationPrincipal User currentUser, Model model) {
        if (currentUser == null) {
            return "redirect:/login";
        }
        List<Course> courses = courseAccessService.getCoursesByUser(currentUser);
        List<CourseDto> courseDtos = courses.stream()
                .map(courseMapper::toCourseDto)
                .toList();
        model.addAttribute("courses", courseDtos);
        model.addAttribute("title", "Мои курсы");
        model.addAttribute("content", "pages/user/my-courses :: my-courses-content");
        return "layouts/main";
    }
}