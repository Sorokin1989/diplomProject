package com.example.diplomproject.controller;

import com.example.diplomproject.entity.Review;
import com.example.diplomproject.enums.ModerationStatus;
import com.example.diplomproject.service.ReviewService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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
                                 @RequestParam(required = false) Integer minRating,
                                 @RequestParam(required = false) String modStatus,
                                 HttpServletRequest request) {
        try {
            List<Review> reviews = reviewService.filterReviews(courseId, userId, minRating, modStatus);
            model.addAttribute("reviews", reviews);
            model.addAttribute("filterCourseId", courseId);
            model.addAttribute("filterUserId", userId);
            model.addAttribute("filterMinRating", minRating);
            model.addAttribute("filterModStatus", modStatus);

            // Формируем текущий URL с параметрами фильтрации, исключая success/error
            String queryString = request.getQueryString();
            if (queryString != null) {
                // Удаляем параметры success и error, чтобы они не накапливались
                queryString = Arrays.stream(queryString.split("&"))
                        .filter(param -> !param.startsWith("success=") && !param.startsWith("error="))
                        .collect(Collectors.joining("&"));
            }
            String currentUrl = UriComponentsBuilder.fromPath(request.getRequestURI())
                    .query(queryString)
                    .build()
                    .toUriString();
            model.addAttribute("currentUrl", currentUrl);
        } catch (Exception e) {
            model.addAttribute("error", "Ошибка загрузки отзывов: " + e.getMessage());
        }
        model.addAttribute("content", "pages/admin/reviews/list :: admin-reviews-content");
        return "layouts/main";
    }

    @GetMapping("/{reviewId}")
    public String viewReview(@PathVariable Long reviewId, Model model) {
        try {
            Review review = reviewService.getReviewEntityById(reviewId);
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
            Review review = reviewService.getReviewEntityById(reviewId);
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
    public String deleteReview(@PathVariable Long reviewId,
                               @RequestParam(required = false) String redirect) {
        try {
            reviewService.deleteReviewById(reviewId);
            String redirectUrl = (redirect != null && !redirect.isBlank()) ? redirect : "/admin/reviews";
            return "redirect:" + addQueryParam(redirectUrl, "success", "Отзыв успешно удалён");
        } catch (Exception e) {
            String redirectUrl = (redirect != null && !redirect.isBlank()) ? redirect : "/admin/reviews";
            return "redirect:" + addQueryParam(redirectUrl, "error", "Ошибка удаления: " + e.getMessage());
        }
    }

    @PostMapping("/{reviewId}/hide")
    public String hideReview(@PathVariable Long reviewId,
                             @RequestParam(required = false) String redirect) {
        try {
            reviewService.hideReview(reviewId);
            String redirectUrl = (redirect != null && !redirect.isBlank()) ? redirect : "/admin/reviews";
            return "redirect:" + addQueryParam(redirectUrl, "success", "Отзыв скрыт");
        } catch (Exception e) {
            String redirectUrl = (redirect != null && !redirect.isBlank()) ? redirect : "/admin/reviews";
            return "redirect:" + addQueryParam(redirectUrl, "error", "Ошибка при скрытии: " + e.getMessage());
        }
    }

    @PostMapping("/{reviewId}/show")
    public String showReview(@PathVariable Long reviewId,
                             @RequestParam(required = false) String redirect) {
        try {
            reviewService.showReview(reviewId);
            String redirectUrl = (redirect != null && !redirect.isBlank()) ? redirect : "/admin/reviews";
            return "redirect:" + addQueryParam(redirectUrl, "success", "Отзыв снова видим");
        } catch (Exception e) {
            String redirectUrl = (redirect != null && !redirect.isBlank()) ? redirect : "/admin/reviews";
            return "redirect:" + addQueryParam(redirectUrl, "error", "Ошибка при показе: " + e.getMessage());
        }
    }

    @PostMapping("/{reviewId}/moderate")
    public String moderateReview(@PathVariable Long reviewId,
                                 @RequestParam ModerationStatus status,
                                 @RequestParam(required = false) String redirect) {
        try {
            reviewService.moderateReview(reviewId, status);
            String successMsg = status == ModerationStatus.APPROVED ? "Отзыв одобрен" : "Отзыв отклонён";
            String redirectUrl = (redirect != null && !redirect.isBlank()) ? redirect : "/admin/reviews";
            return "redirect:" + addQueryParam(redirectUrl, "success", successMsg);
        } catch (Exception e) {
            String redirectUrl = (redirect != null && !redirect.isBlank()) ? redirect : "/admin/reviews";
            return "redirect:" + addQueryParam(redirectUrl, "error", "Ошибка модерации: " + e.getMessage());
        }
    }

    // ==================== Вспомогательные методы ====================

    private String encode(String message) {
        return URLEncoder.encode(message, StandardCharsets.UTF_8);
    }

    private String addQueryParam(String url, String paramName, String paramValue) {
        if (url == null || url.isBlank()) url = "/admin/reviews";
        String encodedValue = encode(paramValue);
        if (url.contains("?")) {
            // Если параметр с таким именем уже есть, заменяем его (опционально)
            // Для простоты добавляем &, но можно реализовать замену
            return url + "&" + paramName + "=" + encodedValue;
        } else {
            return url + "?" + paramName + "=" + encodedValue;
        }
    }
}