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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/course-access")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCourseAccessController {

    private final CourseAccessService courseAccessService;
    private final CourseService courseService;
    private final UserService userService;
    private final OrderRepository orderRepository;

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
        model.addAttribute("title", "Управление доступом к курсам");
        model.addAttribute("content", "pages/admin/course-access/courses :: admin-course-access-content");
        return "layouts/main";
    }

    // Страница управления доступом для конкретного курса
    @GetMapping("/course/{courseId}")
    public String manageCourseAccess(@PathVariable Long courseId, Model model) {
        Course course = courseService.getCourseEntityById(courseId);
        if (course == null) {
            return "redirect:/admin/course-access";
        }
        List<User> usersWithAccess = courseAccessService.getUsersByCourse(course);
        List<User> allUsers = userService.getAllUsers();
        model.addAttribute("course", course);
        model.addAttribute("usersWithAccess", usersWithAccess);
        model.addAttribute("allUsers", allUsers);
        model.addAttribute("title", "Управление доступом: " + course.getTitle());
        model.addAttribute("content", "pages/admin/course-access/manage :: admin-course-access-manage");
        return "layouts/main";
    }

    // Выдать доступ пользователю к курсу
    @PostMapping("/grant")
    public String grantAccess(@RequestParam Long courseId,
                              @RequestParam Long userId,
                              @RequestParam Long orderId,
                              RedirectAttributes redirectAttributes) {
        try {
            Course course = courseService.getCourseEntityById(courseId);
            User user = userService.getUserById(userId);
            Order order = orderRepository.findById(orderId).orElse(null);

            if (course == null || user == null || order == null) {
                redirectAttributes.addFlashAttribute("error", "Курс, пользователь или заказ не найдены");
                return "redirect:/admin/course-access/course/" + courseId;
            }
            if (!order.getUser().getId().equals(userId)) {
                redirectAttributes.addFlashAttribute("error", "Заказ не соответствует пользователю или курсу");
                return "redirect:/admin/course-access/course/" + courseId;
            }

            courseAccessService.grantAccessToCourse(user, course, order);
            redirectAttributes.addFlashAttribute("success", "Доступ успешно выдан");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка: " + e.getMessage());
        }
        return "redirect:/admin/course-access/course/" + courseId;
    }

    // Отозвать доступ
    @PostMapping("/revoke")
    public String revokeAccess(@RequestParam Long courseId,
                               @RequestParam Long userId,
                               RedirectAttributes redirectAttributes) {
        try {
            Course course = courseService.getCourseEntityById(courseId);
            User user = userService.getUserById(userId);
            if (course == null || user == null) {
                redirectAttributes.addFlashAttribute("error", "Курс или пользователь не найдены");
                return "redirect:/admin/course-access/course/" + courseId;
            }
            courseAccessService.revokeAccess(user, course);
            redirectAttributes.addFlashAttribute("success", "Доступ успешно отозван");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка: " + e.getMessage());
        }
        return "redirect:/admin/course-access/course/" + courseId;
    }
}