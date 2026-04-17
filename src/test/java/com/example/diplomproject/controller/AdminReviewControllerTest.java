package com.example.diplomproject.controller;

import com.example.diplomproject.entity.Review;
import com.example.diplomproject.enums.ModerationStatus;
import com.example.diplomproject.service.ReviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AdminReviewControllerTest {

    @Mock
    private ReviewService reviewService;

    @InjectMocks
    private AdminReviewController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    // ---------- GET /admin/reviews ----------
    @Test
    void listAllReviews_shouldReturnViewWithReviews() throws Exception {
        List<Review> reviews = Arrays.asList(new Review(), new Review());
        when(reviewService.filterReviews(any(), any(), any(), any())).thenReturn(reviews);

        mockMvc.perform(get("/admin/reviews"))
                .andExpect(status().isOk())
                .andExpect(view().name("layouts/main"))
                .andExpect(model().attributeExists("reviews", "content"))
                .andExpect(model().attribute("content", "pages/admin/reviews/list :: admin-reviews-content"));

        verify(reviewService).filterReviews(null, null, null, null);
    }

    @Test
    void listAllReviews_withFilters() throws Exception {
        List<Review> filtered = List.of(new Review());
        when(reviewService.filterReviews(eq(1L), eq(2L), eq(3), eq("APPROVED"))).thenReturn(filtered);

        mockMvc.perform(get("/admin/reviews")
                        .param("courseId", "1")
                        .param("userId", "2")
                        .param("minRating", "3")
                        .param("modStatus", "APPROVED"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("filterCourseId", 1L))
                .andExpect(model().attribute("filterUserId", 2L))
                .andExpect(model().attribute("filterMinRating", 3))
                .andExpect(model().attribute("filterModStatus", "APPROVED"));
    }

    @Test
    void listAllReviews_serviceThrowsException() throws Exception {
        when(reviewService.filterReviews(any(), any(), any(), any()))
                .thenThrow(new RuntimeException("DB error"));

        mockMvc.perform(get("/admin/reviews"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("error"));
    }

    // ---------- GET /admin/reviews/{reviewId} ----------
    @Test
    void viewReview_found() throws Exception {
        Review review = new Review();
        review.setId(1L);
        when(reviewService.getReviewEntityById(1L)).thenReturn(review);

        mockMvc.perform(get("/admin/reviews/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("layouts/main"))
                .andExpect(model().attributeExists("review", "content"))
                .andExpect(model().attribute("content", "pages/admin/reviews/view :: admin-review-view-content"));
    }

    @Test
    void viewReview_notFound() throws Exception {
        when(reviewService.getReviewEntityById(99L)).thenThrow(new RuntimeException());

        mockMvc.perform(get("/admin/reviews/99"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/admin/reviews?error=*"));
    }

    // ---------- GET /admin/reviews/{reviewId}/edit ----------
    @Test
    void editReviewForm_found() throws Exception {
        Review review = new Review();
        review.setId(1L);
        when(reviewService.getReviewEntityById(1L)).thenReturn(review);

        mockMvc.perform(get("/admin/reviews/1/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("layouts/main"))
                .andExpect(model().attributeExists("review", "content"))
                .andExpect(model().attribute("content", "pages/admin/reviews/form :: admin-review-form-content"));
    }

    @Test
    void editReviewForm_notFound() throws Exception {
        when(reviewService.getReviewEntityById(99L)).thenThrow(new RuntimeException());

        mockMvc.perform(get("/admin/reviews/99/edit"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/admin/reviews?error=*"));
    }

    // ---------- POST /admin/reviews/{reviewId}/edit ----------
    @Test
    void updateReview_success() throws Exception {
        doNothing().when(reviewService).updateReviewByAdmin(1L, "New text", 4);

        mockMvc.perform(post("/admin/reviews/1/edit")
                        .param("text", "New text")
                        .param("rating", "4"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/admin/reviews/1?success=*"));
    }

    @Test
    void updateReview_invalidRating() throws Exception {
        mockMvc.perform(post("/admin/reviews/1/edit")
                        .param("text", "Some text")
                        .param("rating", "6"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/admin/reviews/1/edit?error=*"));
    }

    @Test
    void updateReview_emptyText() throws Exception {
        mockMvc.perform(post("/admin/reviews/1/edit")
                        .param("text", "")
                        .param("rating", "4"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/admin/reviews/1/edit?error=*"));
    }

    @Test
    void updateReview_serviceThrowsException() throws Exception {
        doThrow(new RuntimeException("Update error"))
                .when(reviewService).updateReviewByAdmin(1L, "Text", 5);

        mockMvc.perform(post("/admin/reviews/1/edit")
                        .param("text", "Text")
                        .param("rating", "5"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/admin/reviews/1/edit?error=*"));
    }

    // ---------- POST /admin/reviews/{reviewId}/delete ----------
    @Test
    void deleteReview_success_withoutRedirectParam() throws Exception {
        doNothing().when(reviewService).deleteReviewById(1L);

        mockMvc.perform(post("/admin/reviews/1/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/admin/reviews?success=*"));
    }

    @Test
    void deleteReview_success_withRedirectParam() throws Exception {
        doNothing().when(reviewService).deleteReviewById(1L);

        mockMvc.perform(post("/admin/reviews/1/delete")
                        .param("redirect", "/admin/reviews?courseId=1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/admin/reviews?courseId=1&success=*"));
    }

    @Test
    void deleteReview_failure() throws Exception {
        doThrow(new RuntimeException("Delete error")).when(reviewService).deleteReviewById(1L);

        mockMvc.perform(post("/admin/reviews/1/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/admin/reviews?error=*"));
    }

    // ---------- POST /admin/reviews/{reviewId}/hide ----------
    @Test
    void hideReview_success() throws Exception {
        doNothing().when(reviewService).hideReview(1L);

        mockMvc.perform(post("/admin/reviews/1/hide"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/admin/reviews?success=*"));
    }

    @Test
    void hideReview_failure() throws Exception {
        doThrow(new RuntimeException("Hide error")).when(reviewService).hideReview(1L);

        mockMvc.perform(post("/admin/reviews/1/hide"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/admin/reviews?error=*"));
    }

    // ---------- POST /admin/reviews/{reviewId}/show ----------
    @Test
    void showReview_success() throws Exception {
        doNothing().when(reviewService).showReview(1L);

        mockMvc.perform(post("/admin/reviews/1/show"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/admin/reviews?success=*"));
    }

    @Test
    void showReview_failure() throws Exception {
        doThrow(new RuntimeException("Show error")).when(reviewService).showReview(1L);

        mockMvc.perform(post("/admin/reviews/1/show"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/admin/reviews?error=*"));
    }

    // ---------- POST /admin/reviews/{reviewId}/moderate ----------
    @Test
    void moderateReview_approved_success() throws Exception {
        doNothing().when(reviewService).moderateReview(1L, ModerationStatus.APPROVED);

        mockMvc.perform(post("/admin/reviews/1/moderate")
                        .param("status", "APPROVED"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/admin/reviews?success=*"));
    }

    @Test
    void moderateReview_rejected_success() throws Exception {
        doNothing().when(reviewService).moderateReview(1L, ModerationStatus.REJECTED);

        mockMvc.perform(post("/admin/reviews/1/moderate")
                        .param("status", "REJECTED"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/admin/reviews?success=*"));
    }

    @Test
    void moderateReview_failure() throws Exception {
        doThrow(new RuntimeException("Moderate error"))
                .when(reviewService).moderateReview(1L, ModerationStatus.APPROVED);

        mockMvc.perform(post("/admin/reviews/1/moderate")
                        .param("status", "APPROVED"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/admin/reviews?error=*"));
    }
}