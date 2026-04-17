package com.example.diplomproject.controller;

import com.example.diplomproject.dto.CourseDto;
import com.example.diplomproject.dto.ReviewDto;
import com.example.diplomproject.entity.User;
import com.example.diplomproject.enums.Role;
import com.example.diplomproject.service.CourseService;
import com.example.diplomproject.service.OrderService;
import com.example.diplomproject.service.ReviewService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ReviewControllerTest {

    @Mock
    private ReviewService reviewService;

    @Mock
    private CourseService courseService;

    @Mock
    private OrderService orderService;

    @InjectMocks
    private ReviewController controller;

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

    // ---------- GET /courses/{courseId}/reviews ----------
    @Test
    void listReviews_success() throws Exception {
        Long courseId = 1L;
        CourseDto courseDto = new CourseDto();
        courseDto.setId(courseId);
        courseDto.setTitle("Test Course");
        when(courseService.getCourseDtoById(courseId)).thenReturn(courseDto);
        when(reviewService.getVisibleReviewDtosByCourseId(courseId, null)).thenReturn(List.of());
        when(reviewService.averageRatingForCourse(courseId)).thenReturn(4.5);

        mockMvc.perform(get("/courses/1/reviews"))
                .andExpect(status().isOk())
                .andExpect(view().name("layouts/main"))
                .andExpect(model().attributeExists("course", "reviews", "averageRating", "canReview", "title", "content"))
                .andExpect(model().attribute("title", "Отзывы о курсе «Test Course»"));
    }

    @Test
    void listReviews_courseNotFound() throws Exception {
        when(courseService.getCourseDtoById(99L)).thenThrow(new IllegalArgumentException());

        mockMvc.perform(get("/courses/99/reviews"))
                .andExpect(status().isNotFound());
    }

    @Test
    void listReviews_authenticatedUser_canReview() throws Exception {
        Long courseId = 1L;
        User user = new User();
        user.setId(1L);
        user.setRole(Role.USER);
        authenticateUser(user);

        CourseDto courseDto = new CourseDto();
        courseDto.setId(courseId);
        courseDto.setTitle("Test Course");
        when(courseService.getCourseDtoById(courseId)).thenReturn(courseDto);
        when(reviewService.getVisibleReviewDtosByCourseId(courseId, user)).thenReturn(List.of());
        when(reviewService.averageRatingForCourse(courseId)).thenReturn(4.5);
        when(reviewService.hasUserActiveReview(user.getId(), courseId)).thenReturn(false);

        mockMvc.perform(get("/courses/1/reviews"))
                .andExpect(model().attribute("canReview", true));

        clearAuthentication();
    }

    // ---------- GET /courses/{courseId}/reviews/new ----------
    @Test
    void showCreateForm_authenticatedPurchased_shouldReturnForm() throws Exception {
        Long courseId = 1L;
        User user = new User();
        user.setId(1L);
        user.setRole(Role.USER);
        authenticateUser(user);

        CourseDto courseDto = new CourseDto();
        courseDto.setId(courseId);
        when(courseService.getCourseDtoById(courseId)).thenReturn(courseDto);
        when(orderService.hasUserPurchasedCourse(user.getId(), courseId)).thenReturn(true);
        when(reviewService.hasUserReviewedCourse(user.getId(), courseId)).thenReturn(false);

        mockMvc.perform(get("/courses/1/reviews/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("layouts/main"))
                .andExpect(model().attributeExists("course", "reviewDto", "title", "content"))
                .andExpect(model().attribute("title", "Написать отзыв"));

        clearAuthentication();
    }

    @Test
    void showCreateForm_unauthenticated_shouldRedirectToLogin() throws Exception {
        mockMvc.perform(get("/courses/1/reviews/new"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void showCreateForm_notPurchased_shouldRedirectWithError() throws Exception {
        Long courseId = 1L;
        User user = new User();
        user.setId(1L);
        user.setRole(Role.USER);
        authenticateUser(user);

        CourseDto courseDto = new CourseDto();
        courseDto.setId(courseId);
        when(courseService.getCourseDtoById(courseId)).thenReturn(courseDto);
        when(orderService.hasUserPurchasedCourse(user.getId(), courseId)).thenReturn(false);

        mockMvc.perform(get("/courses/1/reviews/new"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/courses/1?error=*"));

        clearAuthentication();
    }

    @Test
    void showCreateForm_alreadyReviewed_shouldRedirectWithError() throws Exception {
        Long courseId = 1L;
        User user = new User();
        user.setId(1L);
        user.setRole(Role.USER);
        authenticateUser(user);

        CourseDto courseDto = new CourseDto();
        courseDto.setId(courseId);
        when(courseService.getCourseDtoById(courseId)).thenReturn(courseDto);
        when(orderService.hasUserPurchasedCourse(user.getId(), courseId)).thenReturn(true);
        when(reviewService.hasUserReviewedCourse(user.getId(), courseId)).thenReturn(true);

        mockMvc.perform(get("/courses/1/reviews/new"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/courses/1?error=*"));

        clearAuthentication();
    }

    // ---------- POST /courses/{courseId}/reviews ----------
    @Test
    void createReview_success() throws Exception {
        Long courseId = 1L;
        User user = new User();
        user.setId(1L);
        user.setRole(Role.USER);
        authenticateUser(user);

        doNothing().when(reviewService).createReview(user.getId(), courseId, "Great!", 5);

        mockMvc.perform(post("/courses/1/reviews")
                        .param("text", "Great!")
                        .param("rating", "5"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/courses/1?success=*"));

        clearAuthentication();
    }

    @Test
    void createReview_invalidRating_shouldRedirectWithError() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setRole(Role.USER);
        authenticateUser(user);

        mockMvc.perform(post("/courses/1/reviews")
                        .param("text", "Great!")
                        .param("rating", "6"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/courses/1?error=*"));

        clearAuthentication();
    }

    @Test
    void createReview_emptyText_shouldRedirectWithError() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setRole(Role.USER);
        authenticateUser(user);

        mockMvc.perform(post("/courses/1/reviews")
                        .param("text", "")
                        .param("rating", "5"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/courses/1?error=*"));

        clearAuthentication();
    }

    // ---------- GET /courses/{courseId}/reviews/{reviewId}/edit ----------
    @Test
    void showEditForm_owner_shouldReturnForm() throws Exception {
        Long courseId = 1L;
        Long reviewId = 10L;
        User user = new User();
        user.setId(1L);
        user.setRole(Role.USER);
        authenticateUser(user);

        CourseDto courseDto = new CourseDto();
        courseDto.setId(courseId);
        ReviewDto reviewDto = new ReviewDto();
        reviewDto.setId(reviewId);
        reviewDto.setCourseId(courseId);
        reviewDto.setUserId(user.getId());
        when(courseService.getCourseDtoById(courseId)).thenReturn(courseDto);
        when(reviewService.getReviewDtoById(reviewId)).thenReturn(reviewDto);

        mockMvc.perform(get("/courses/1/reviews/10/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("layouts/main"))
                .andExpect(model().attributeExists("reviewDto", "course", "title", "content"))
                .andExpect(model().attribute("title", "Редактирование отзыва"));

        clearAuthentication();
    }

    @Test
    void showEditForm_admin_shouldReturnForm() throws Exception {
        Long courseId = 1L;
        Long reviewId = 10L;
        User admin = new User();
        admin.setId(2L);
        admin.setRole(Role.ADMIN);
        authenticateUser(admin);

        CourseDto courseDto = new CourseDto();
        courseDto.setId(courseId);
        ReviewDto reviewDto = new ReviewDto();
        reviewDto.setId(reviewId);
        reviewDto.setCourseId(courseId);
        reviewDto.setUserId(1L); // чужой
        when(courseService.getCourseDtoById(courseId)).thenReturn(courseDto);
        when(reviewService.getReviewDtoById(reviewId)).thenReturn(reviewDto);

        mockMvc.perform(get("/courses/1/reviews/10/edit"))
                .andExpect(status().isOk());

        clearAuthentication();
    }

    @Test
    void showEditForm_forbidden() throws Exception {
        Long courseId = 1L;
        Long reviewId = 10L;
        User stranger = new User();
        stranger.setId(3L);
        stranger.setRole(Role.USER);
        authenticateUser(stranger);

        ReviewDto reviewDto = new ReviewDto();
        reviewDto.setId(reviewId);
        reviewDto.setCourseId(courseId);
        reviewDto.setUserId(1L); // чужой
        when(reviewService.getReviewDtoById(reviewId)).thenReturn(reviewDto);

        mockMvc.perform(get("/courses/1/reviews/10/edit"))
                .andExpect(status().is5xxServerError());

        clearAuthentication();
    }

    // ---------- POST /courses/{courseId}/reviews/{reviewId} ----------
    @Test
    void updateReview_success() throws Exception {
        Long courseId = 1L;
        Long reviewId = 10L;
        User user = new User();
        user.setId(1L);
        user.setRole(Role.USER);
        authenticateUser(user);

        ReviewDto existing = new ReviewDto();
        existing.setId(reviewId);
        existing.setCourseId(courseId);
        existing.setUserId(user.getId());
        when(reviewService.getReviewDtoById(reviewId)).thenReturn(existing);
        doNothing().when(reviewService).updateReview(reviewId, "Updated", 4, user.getId());

        mockMvc.perform(post("/courses/1/reviews/10")
                        .param("text", "Updated")
                        .param("rating", "4"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/courses/1?success=*"));

        clearAuthentication();
    }

    @Test
    void updateReview_invalidRating_shouldRedirectWithError() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setRole(Role.USER);
        authenticateUser(user);

        mockMvc.perform(post("/courses/1/reviews/10")
                        .param("text", "Updated")
                        .param("rating", "6"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/courses/1?error=*"));

        clearAuthentication();
    }

    // ---------- POST /courses/{courseId}/reviews/{reviewId}/delete ----------
    @Test
    void deleteReview_owner_success() throws Exception {
        Long courseId = 1L;
        Long reviewId = 10L;
        User user = new User();
        user.setId(1L);
        user.setRole(Role.USER);
        authenticateUser(user);

        ReviewDto existing = new ReviewDto();
        existing.setId(reviewId);
        existing.setCourseId(courseId);
        existing.setUserId(user.getId());
        when(reviewService.getReviewDtoById(reviewId)).thenReturn(existing);
        doNothing().when(reviewService).deleteReview(reviewId, user.getId());

        mockMvc.perform(post("/courses/1/reviews/10/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/courses/1?success=*"));

        clearAuthentication();
    }

    @Test
    void deleteReview_forbidden() throws Exception {
        Long courseId = 1L;
        Long reviewId = 10L;
        User stranger = new User();
        stranger.setId(3L);
        stranger.setRole(Role.USER);
        authenticateUser(stranger);

        ReviewDto existing = new ReviewDto();
        existing.setId(reviewId);
        existing.setCourseId(courseId);
        existing.setUserId(1L); // чужой
        when(reviewService.getReviewDtoById(reviewId)).thenReturn(existing);

        mockMvc.perform(post("/courses/1/reviews/10/delete"))
                .andExpect(status().isForbidden());

        clearAuthentication();
    }
}