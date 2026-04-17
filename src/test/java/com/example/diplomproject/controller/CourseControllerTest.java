package com.example.diplomproject.controller;

import com.example.diplomproject.dto.CourseDto;
import com.example.diplomproject.entity.Category;
import com.example.diplomproject.entity.Certificate;
import com.example.diplomproject.entity.Course;
import com.example.diplomproject.entity.User;
import com.example.diplomproject.enums.Role;
import com.example.diplomproject.mapper.CategoryMapper;
import com.example.diplomproject.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;
import java.util.Collections;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CourseControllerTest {

    @Mock
    private CourseService courseService;

    @Mock
    private CategoryService categoryService;

    @Mock
    private CategoryMapper categoryMapper;

    @Mock
    private ReviewService reviewService;

    @Mock
    private CourseAccessService courseAccessService;

    @Mock
    private CertificateService certificateService;

    @InjectMocks
    private CourseController controller;

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

    // ---------- GET /courses ----------
    @Test
    void listCourses_noFilter_shouldReturnAllCourses() throws Exception {
        CourseDto dto = new CourseDto();
        dto.setId(1L);
        when(courseService.getAllCourses()).thenReturn(List.of(dto));

        mockMvc.perform(get("/courses"))
                .andExpect(status().isOk())
                .andExpect(view().name("layouts/main"))
                .andExpect(model().attributeExists("courses", "title", "content"))
                .andExpect(model().attribute("courses", List.of(dto)))
                .andExpect(model().attribute("title", "Курсы"))
                .andExpect(model().attribute("content", "pages/courses/courses :: user-courses-content"));

        verify(courseService).getAllCourses();
    }

    @Test
    void listCourses_withValidCategory_shouldReturnFilteredCourses() throws Exception {
        Long categoryId = 1L;
        Category category = new Category();
        category.setId(categoryId);
        category.setTitle("Test");
        when(categoryService.getCategoryById(categoryId)).thenReturn(category);
        when(categoryMapper.toCategoryDTO(category)).thenReturn(new com.example.diplomproject.dto.CategoryDto());
        when(courseService.getCourseDtosByCategoryId(categoryId)).thenReturn(List.of(new CourseDto()));

        mockMvc.perform(get("/courses").param("categoryId", "1"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("category"))
                .andExpect(model().attributeExists("courses"));
    }

    @Test
    void listCourses_withInvalidCategory_shouldReturnEmptyList() throws Exception {
        when(categoryService.getCategoryById(99L)).thenThrow(new IllegalArgumentException());

        mockMvc.perform(get("/courses").param("categoryId", "99"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("courses", Collections.emptyList()));
    }

    // ---------- GET /courses/{id} ----------
    @Test
    void viewCourse_courseNotFound_shouldReturn404() throws Exception {
        when(courseService.getCourseDtoById(1L)).thenThrow(new IllegalArgumentException());

        mockMvc.perform(get("/courses/1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void viewCourse_unauthenticatedUser() throws Exception {
        CourseDto courseDto = new CourseDto();
        courseDto.setId(1L);
        when(courseService.getCourseDtoById(1L)).thenReturn(courseDto);
        when(reviewService.getVisibleReviewDtosByCourseId(1L, null)).thenReturn(List.of());
        when(reviewService.averageRatingForCourse(1L)).thenReturn(4.5);
        when(reviewService.getApprovedReviewCount(1L)).thenReturn(5);

        mockMvc.perform(get("/courses/1"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("purchased", false))
                .andExpect(model().attribute("hasCertificate", false))
                .andExpect(model().attribute("hasMaterials", false))
                .andExpect(model().attribute("canReview", false));
    }

    @Test
    void viewCourse_authenticatedUser_purchasedButNoCertificateNoMaterials() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setRole(Role.USER);
        authenticateUser(user);

        CourseDto courseDto = new CourseDto();
        courseDto.setId(1L);
        courseDto.setMaterialsPath(null);
        when(courseService.getCourseDtoById(1L)).thenReturn(courseDto);
        when(reviewService.getVisibleReviewDtosByCourseId(1L, user)).thenReturn(List.of());
        when(reviewService.averageRatingForCourse(1L)).thenReturn(4.5);
        when(reviewService.getApprovedReviewCount(1L)).thenReturn(5);
        when(courseAccessService.hasAccessToUserForDto(user, courseDto)).thenReturn(true);
        when(certificateService.findByUserAndCourse(1L, 1L)).thenReturn(null);
        when(reviewService.hasUserActiveReview(1L, 1L)).thenReturn(false);

        mockMvc.perform(get("/courses/1"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("purchased", true))
                .andExpect(model().attribute("hasCertificate", false))
                .andExpect(model().attribute("hasMaterials", false))
                .andExpect(model().attribute("canReview", true));

        clearAuthentication();
    }

    @Test
    void viewCourse_authenticatedUser_purchasedWithCertificateAndMaterials() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setRole(Role.USER);
        authenticateUser(user);

        CourseDto courseDto = new CourseDto();
        courseDto.setId(1L);
        courseDto.setMaterialsPath("path/to/materials.zip");
        when(courseService.getCourseDtoById(1L)).thenReturn(courseDto);
        when(reviewService.getVisibleReviewDtosByCourseId(1L, user)).thenReturn(List.of());
        when(reviewService.averageRatingForCourse(1L)).thenReturn(4.5);
        when(reviewService.getApprovedReviewCount(1L)).thenReturn(5);
        when(courseAccessService.hasAccessToUserForDto(user, courseDto)).thenReturn(true);
        Certificate cert = new Certificate();
        cert.setRevoked(false);
        cert.setCertificateUrl("cert.pdf");
        when(certificateService.findByUserAndCourse(1L, 1L)).thenReturn(cert);
        when(reviewService.hasUserActiveReview(1L, 1L)).thenReturn(false);

        mockMvc.perform(get("/courses/1"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("purchased", true))
                .andExpect(model().attribute("hasCertificate", true))
                .andExpect(model().attribute("hasMaterials", true))
                .andExpect(model().attribute("canReview", true));

        clearAuthentication();
    }

    @Test
    void viewCourse_authenticatedUser_alreadyReviewed_shouldNotAllowReview() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setRole(Role.USER);
        authenticateUser(user);

        CourseDto courseDto = new CourseDto();
        courseDto.setId(1L);
        when(courseService.getCourseDtoById(1L)).thenReturn(courseDto);
        when(reviewService.getVisibleReviewDtosByCourseId(1L, user)).thenReturn(List.of());
        when(reviewService.averageRatingForCourse(1L)).thenReturn(4.5);
        when(reviewService.getApprovedReviewCount(1L)).thenReturn(5);
        when(courseAccessService.hasAccessToUserForDto(user, courseDto)).thenReturn(true);
        when(reviewService.hasUserActiveReview(1L, 1L)).thenReturn(true);

        mockMvc.perform(get("/courses/1"))
                .andExpect(model().attribute("canReview", false));

        clearAuthentication();
    }

    // ---------- GET /courses/search ----------
    @Test
    void searchCourses_withTitle() throws Exception {
        List<CourseDto> found = List.of(new CourseDto());
        when(courseService.searchCourseDtosByTitle("spring")).thenReturn(found);

        mockMvc.perform(get("/courses/search").param("title", "spring"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("courses", found))
                .andExpect(model().attribute("searchTitle", "spring"))
                .andExpect(model().attribute("title", "Результаты поиска: spring"));
    }

    @Test
    void searchCourses_withoutTitle() throws Exception {
        List<CourseDto> all = List.of(new CourseDto());
        when(courseService.searchCourseDtosByTitle(null)).thenReturn(all);

        mockMvc.perform(get("/courses/search"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("courses", all))
                .andExpect(model().attribute("searchTitle", nullValue()))   // проверяем null
                .andExpect(model().attribute("title", "Все курсы"));
    }

    // ---------- GET /courses/{id}/download-materials ----------
    @Test
    void downloadMaterials_unauthenticated_shouldReturnUnauthorized() {
        assertThrows(ResponseStatusException.class, () -> {
            controller.downloadCourseMaterials(1L, null);
        });
    }

    @Test
    void downloadMaterials_courseNotFound_shouldReturnNotFound() {
        User user = new User();
        user.setId(1L);
        when(courseService.getCourseEntityById(1L)).thenThrow(new IllegalArgumentException());
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            controller.downloadCourseMaterials(1L, user);
        });
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void downloadMaterials_noAccess_shouldReturnForbidden() {
        User user = new User();
        user.setId(1L);
        Course course = new Course();
        when(courseService.getCourseEntityById(1L)).thenReturn(course);
        when(courseAccessService.hasAccessToUser(user, course)).thenReturn(false);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            controller.downloadCourseMaterials(1L, user);
        });
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void downloadMaterials_noMaterialsPath_shouldReturnNotFound() {
        User user = new User();
        user.setId(1L);
        Course course = new Course();
        course.setMaterialsPath(null);
        when(courseService.getCourseEntityById(1L)).thenReturn(course);
        when(courseAccessService.hasAccessToUser(user, course)).thenReturn(true);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            controller.downloadCourseMaterials(1L, user);
        });
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}