package com.example.diplomproject.controller;

import com.example.diplomproject.entity.Course;
import com.example.diplomproject.entity.Order;
import com.example.diplomproject.entity.User;
import com.example.diplomproject.repository.OrderRepository;
import com.example.diplomproject.service.CourseAccessService;
import com.example.diplomproject.service.CourseService;
import com.example.diplomproject.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/admin/course-access")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCourseAccessController {

    private final CourseAccessService courseAccessService;
    private final CourseService courseService;
    private final UserService userService;
    private  final OrderRepository orderRepository;

    @Autowired
    public AdminCourseAccessController(CourseAccessService courseAccessService,
                                       CourseService courseService,
                                       UserService userService, OrderRepository orderRepository) {
        this.courseAccessService = courseAccessService;
        this.courseService = courseService;
        this.userService = userService;
        this.orderRepository = orderRepository;
    }

    // Список всех курсов с возможностью управления доступом (используем админский метод)
    @GetMapping
    public String listCourses(Model model) {
        model.addAttribute("courses", courseService.getAllCoursesForAdmin());
        return "pages/admin/course-access/courses";
    }

    // Страница управления доступом для конкретного курса
    @GetMapping("/course/{courseId}")
    public String manageCourseAccess(@PathVariable Long courseId, Model model) {
        Course course = courseService.getCourseEntityById(courseId);
        List<User> usersWithAccess = courseAccessService.getUsersByCourse(course);
        List<User> allUsers = userService.getAllUsers();
        model.addAttribute("course", course);
        model.addAttribute("usersWithAccess", usersWithAccess);
        model.addAttribute("allUsers", allUsers);
        return "pages/admin/course-access/manage";
    }

    // Выдать доступ пользователю к курсу
    @PostMapping("/grant")
    public String grantAccess(@RequestParam Long courseId, @RequestParam Long userId, @RequestParam Long orderId) {
        Course course = courseService.getCourseEntityById(courseId);
        User user = userService.getUserById(userId);
        Order order=orderRepository.getOrderById(orderId);
        courseAccessService.grantAccessToCourse(user, course,order);
        return "redirect:/admin/course-access/course/" + courseId;
    }

    // Отозвать доступ
    @PostMapping("/revoke")
    public String revokeAccess(@RequestParam Long courseId, @RequestParam Long userId) {
        Course course = courseService.getCourseEntityById(courseId);
        User user = userService.getUserById(userId);
        courseAccessService.revokeAccess(user, course);
        return "redirect:/admin/course-access/course/" + courseId;
    }
}