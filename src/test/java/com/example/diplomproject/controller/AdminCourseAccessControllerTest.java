package com.example.diplomproject.controller;

import com.example.diplomproject.entity.Course;
import com.example.diplomproject.entity.Order;
import com.example.diplomproject.entity.User;
import com.example.diplomproject.repository.OrderRepository;
import com.example.diplomproject.service.CourseAccessService;
import com.example.diplomproject.service.CourseService;
import com.example.diplomproject.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AdminCourseAccessControllerTest {

    @Mock
    private CourseAccessService courseAccessService;

    @Mock
    private CourseService courseService;

    @Mock
    private UserService userService;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private AdminCourseAccessController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    // ---------- GET /admin/course-access ----------
    @Test
    void listCourses_shouldReturnViewWithCourses() throws Exception {
        List<Course> courses = List.of(new Course(), new Course());
        when(courseService.getAllCoursesForAdmin()).thenReturn(courses);

        mockMvc.perform(get("/admin/course-access"))
                .andExpect(status().isOk())
                .andExpect(view().name("layouts/main"))
                .andExpect(model().attributeExists("courses"))
                .andExpect(model().attribute("title", "Управление доступом к курсам"))
                .andExpect(model().attribute("content", "pages/admin/course-access/courses :: admin-course-access-content"));

        verify(courseService).getAllCoursesForAdmin();
    }

    // ---------- GET /admin/course-access/course/{courseId} ----------
    @Test
    void manageCourseAccess_courseFound() throws Exception {
        Course course = new Course();
        course.setId(1L);
        course.setTitle("Test Course");
        List<User> usersWithAccess = List.of(new User(), new User());
        List<User> allUsers = List.of(new User(), new User(), new User());

        when(courseService.getCourseEntityById(1L)).thenReturn(course);
        when(courseAccessService.getUsersByCourse(course)).thenReturn(usersWithAccess);
        when(userService.getAllUsers()).thenReturn(allUsers);

        mockMvc.perform(get("/admin/course-access/course/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("layouts/main"))
                .andExpect(model().attribute("course", course))
                .andExpect(model().attribute("usersWithAccess", usersWithAccess))
                .andExpect(model().attribute("allUsers", allUsers))
                .andExpect(model().attribute("title", "Управление доступом: Test Course"))
                .andExpect(model().attribute("content", "pages/admin/course-access/manage :: admin-course-access-manage"));
    }

    @Test
    void manageCourseAccess_courseNotFound() throws Exception {
        when(courseService.getCourseEntityById(99L)).thenReturn(null);

        mockMvc.perform(get("/admin/course-access/course/99"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/course-access"));
    }

    // ---------- POST /admin/course-access/grant ----------
    @Test
    void grantAccess_success() throws Exception {
        Course course = new Course();
        course.setId(1L);
        User user = new User();
        user.setId(1L);
        Order order = new Order();
        order.setId(1L);
        order.setUser(user);

        when(courseService.getCourseEntityById(1L)).thenReturn(course);
        when(userService.getUserById(1L)).thenReturn(user);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        doNothing().when(courseAccessService).grantAccessToCourse(user, course, order);

        mockMvc.perform(post("/admin/course-access/grant")
                        .param("courseId", "1")
                        .param("userId", "1")
                        .param("orderId", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/course-access/course/1"))
                .andExpect(flash().attribute("success", "Доступ успешно выдан"));

        verify(courseAccessService).grantAccessToCourse(user, course, order);
    }

    @Test
    void grantAccess_courseUserOrOrderNotFound() throws Exception {
        when(courseService.getCourseEntityById(1L)).thenReturn(null);

        mockMvc.perform(post("/admin/course-access/grant")
                        .param("courseId", "1")
                        .param("userId", "1")
                        .param("orderId", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/course-access/course/1"))
                .andExpect(flash().attribute("error", "Курс, пользователь или заказ не найдены"));

        verify(courseAccessService, never()).grantAccessToCourse(any(), any(), any());
    }

    @Test
    void grantAccess_orderUserMismatch() throws Exception {
        Course course = new Course();
        course.setId(1L);
        User user = new User();
        user.setId(1L);
        Order order = new Order();
        order.setId(1L);
        User otherUser = new User();
        otherUser.setId(2L);
        order.setUser(otherUser);

        when(courseService.getCourseEntityById(1L)).thenReturn(course);
        when(userService.getUserById(1L)).thenReturn(user);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        mockMvc.perform(post("/admin/course-access/grant")
                        .param("courseId", "1")
                        .param("userId", "1")
                        .param("orderId", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/course-access/course/1"))
                .andExpect(flash().attribute("error", "Заказ не соответствует пользователю или курсу"));

        verify(courseAccessService, never()).grantAccessToCourse(any(), any(), any());
    }

    @Test
    void grantAccess_serviceThrowsException() throws Exception {
        Course course = new Course();
        course.setId(1L);
        User user = new User();
        user.setId(1L);
        Order order = new Order();
        order.setId(1L);
        order.setUser(user);

        when(courseService.getCourseEntityById(1L)).thenReturn(course);
        when(userService.getUserById(1L)).thenReturn(user);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        doThrow(new RuntimeException("DB error")).when(courseAccessService).grantAccessToCourse(user, course, order);

        mockMvc.perform(post("/admin/course-access/grant")
                        .param("courseId", "1")
                        .param("userId", "1")
                        .param("orderId", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/course-access/course/1"))
                .andExpect(flash().attribute("error", "Ошибка: DB error"));
    }

    // ---------- POST /admin/course-access/revoke ----------
    @Test
    void revokeAccess_success() throws Exception {
        Course course = new Course();
        course.setId(1L);
        User user = new User();
        user.setId(1L);

        when(courseService.getCourseEntityById(1L)).thenReturn(course);
        when(userService.getUserById(1L)).thenReturn(user);
        doNothing().when(courseAccessService).revokeAccess(user, course);

        mockMvc.perform(post("/admin/course-access/revoke")
                        .param("courseId", "1")
                        .param("userId", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/course-access/course/1"))
                .andExpect(flash().attribute("success", "Доступ успешно отозван"));

        verify(courseAccessService).revokeAccess(user, course);
    }

    @Test
    void revokeAccess_courseOrUserNotFound() throws Exception {
        when(courseService.getCourseEntityById(1L)).thenReturn(null);

        mockMvc.perform(post("/admin/course-access/revoke")
                        .param("courseId", "1")
                        .param("userId", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/course-access/course/1"))
                .andExpect(flash().attribute("error", "Курс или пользователь не найдены"));

        verify(courseAccessService, never()).revokeAccess(any(), any());
    }

    @Test
    void revokeAccess_serviceThrowsException() throws Exception {
        Course course = new Course();
        course.setId(1L);
        User user = new User();
        user.setId(1L);

        when(courseService.getCourseEntityById(1L)).thenReturn(course);
        when(userService.getUserById(1L)).thenReturn(user);
        doThrow(new RuntimeException("Revoke error")).when(courseAccessService).revokeAccess(user, course);

        mockMvc.perform(post("/admin/course-access/revoke")
                        .param("courseId", "1")
                        .param("userId", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/course-access/course/1"))
                .andExpect(flash().attribute("error", "Ошибка: Revoke error"));
    }
}