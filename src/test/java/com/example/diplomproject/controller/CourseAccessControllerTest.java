package com.example.diplomproject.controller;

import com.example.diplomproject.dto.CourseDto;
import com.example.diplomproject.entity.Course;
import com.example.diplomproject.entity.User;
import com.example.diplomproject.enums.Role;
import com.example.diplomproject.mapper.CourseMapper;
import com.example.diplomproject.service.CourseAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CourseAccessControllerTest {

    @Mock
    private CourseAccessService courseAccessService;

    @Mock
    private CourseMapper courseMapper;

    @InjectMocks
    private CourseAccessController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    private void authenticateUser(User user) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities())
        );
    }

    private void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void showMyCourses_authenticatedUser_shouldReturnPageWithCourses() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setRole(Role.USER);
        authenticateUser(user);

        Course course = new Course();
        course.setId(1L);
        CourseDto courseDto = new CourseDto();
        courseDto.setId(1L);
        when(courseAccessService.getCoursesByUser(user)).thenReturn(List.of(course));
        when(courseMapper.toCourseDto(course)).thenReturn(courseDto);

        mockMvc.perform(get("/my-courses"))
                .andExpect(status().isOk())
                .andExpect(view().name("layouts/main"))
                .andExpect(model().attributeExists("courses", "title", "content"))
                .andExpect(model().attribute("courses", List.of(courseDto)))
                .andExpect(model().attribute("title", "Мои курсы"))
                .andExpect(model().attribute("content", "pages/user/my-courses :: my-courses-content"));

        verify(courseAccessService).getCoursesByUser(user);
        verify(courseMapper).toCourseDto(course);
        clearAuthentication();
    }

    @Test
    void showMyCourses_authenticatedUser_noCourses_shouldReturnEmptyList() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setRole(Role.USER);
        authenticateUser(user);

        when(courseAccessService.getCoursesByUser(user)).thenReturn(List.of());

        mockMvc.perform(get("/my-courses"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("courses", List.of()));

        verify(courseMapper, never()).toCourseDto(any());
        clearAuthentication();
    }

    @Test
    void showMyCourses_unauthenticatedUser_shouldRedirectToLogin() throws Exception {
        mockMvc.perform(get("/my-courses"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }
}