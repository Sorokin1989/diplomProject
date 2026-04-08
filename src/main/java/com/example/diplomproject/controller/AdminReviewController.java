package com.example.diplomproject.controller;

import com.example.diplomproject.entity.Review;
import com.example.diplomproject.service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@PreAuthorize("hasRole('ADMIN')")
@Controller
@RequestMapping("/admin/reviews")
public class AdminReviewController {

    private final ReviewService reviewService;

    @Autowired
    public AdminReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping
    public String listAllReviews(Model model,
                                 @RequestParam(required = false) Long courseId,
                                 @RequestParam(required = false) Long userId,
                                 @RequestParam(required = false) Integer minRating) {
        try {
            List<Review> reviews;
            if (courseId != null) {
                reviews = reviewService.getReviewsByCourseId(courseId);
                model.addAttribute("filterCourseId", courseId);
            } else if (userId != null) {
                reviews = reviewService.getReviewsByUserId(userId);
                model.addAttribute("filterUserId", userId);
            } else if (minRating != null) {
                reviews = reviewService.getReviewsWithMinRating(minRating);
                model.addAttribute("filterMinRating", minRating);
            } else {
                reviews = reviewService.getAllReviews();
            }
            model.addAttribute("reviews", reviews);
        } catch (Exception e) {
            model.addAttribute("error", "Ошибка загрузки отзывов: " + e.getMessage());
        }
        model.addAttribute("content", "pages/admin/reviews/list :: admin-reviews-content");
        return "layouts/main";
    }

    @GetMapping("/{reviewId}")
    public String viewReview(@PathVariable Long reviewId, Model model) {
        try {
            Review review = reviewService.getReviewById(reviewId);
            model.addAttribute("review", review);
            model.addAttribute("content", "pages/admin/reviews/view :: admin-review-view-content");
            return "layouts/main";
        } catch (Exception e) {
            return "redirect:/admin/reviews?error=" + encode("Отзыв не найден");
        }
    }

    @GetMapping("/{reviewId}/edit")
    public String editReviewForm(@PathVariable Long reviewId, Model model) {
        try {
            Review review = reviewService.getReviewById(reviewId);
            model.addAttribute("review", review);
            model.addAttribute("content", "pages/admin/reviews/form :: admin-review-form-content");
            return "layouts/main";
        } catch (Exception e) {
            return "redirect:/admin/reviews?error=" + encode("Отзыв не найден");
        }
    }

    @PostMapping("/{reviewId}/edit")
    public String updateReview(@PathVariable Long reviewId,
                               @RequestParam String text,
                               @RequestParam Integer rating) {
        if (rating == null || rating < 1 || rating > 5) {
            return "redirect:/admin/reviews/" + reviewId + "/edit?error=" + encode("Рейтинг должен быть от 1 до 5");
        }
        if (text == null || text.trim().isEmpty()) {
            return "redirect:/admin/reviews/" + reviewId + "/edit?error=" + encode("Текст отзыва не может быть пустым");
        }
        try {
            reviewService.updateReviewByAdmin(reviewId, text.trim(), rating);
            return "redirect:/admin/reviews/" + reviewId + "?success=" + encode("Отзыв успешно обновлён");
        } catch (Exception e) {
            return "redirect:/admin/reviews/" + reviewId + "/edit?error=" + encode("Ошибка обновления: " + e.getMessage());
        }
    }

    @PostMapping("/{reviewId}/delete")
    public String deleteReview(@PathVariable Long reviewId) {
        try {
            reviewService.deleteReviewById(reviewId);
            return "redirect:/admin/reviews?success=" + encode("Отзыв успешно удалён");
        } catch (Exception e) {
            return "redirect:/admin/reviews?error=" + encode("Ошибка удаления: " + e.getMessage());
        }
    }

    @PostMapping("/{reviewId}/hide")
    public String hideReview(@PathVariable Long reviewId) {
        try {
            reviewService.hideReview(reviewId);
            return "redirect:/admin/reviews?success=" + encode("Отзыв скрыт");
        } catch (Exception e) {
            return "redirect:/admin/reviews?error=" + encode("Ошибка при скрытии: " + e.getMessage());
        }
    }

    @PostMapping("/{reviewId}/show")
    public String showReview(@PathVariable Long reviewId) {
        try {
            reviewService.showReview(reviewId);
            return "redirect:/admin/reviews?success=" + encode("Отзыв снова видим");
        } catch (Exception e) {
            return "redirect:/admin/reviews?error=" + encode("Ошибка при показе: " + e.getMessage());
        }
    }

    private String encode(String message) {
        return URLEncoder.encode(message, StandardCharsets.UTF_8);
    }
}