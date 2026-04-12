package com.example.diplomproject.controller;

import com.example.diplomproject.dto.CourseDto;
import com.example.diplomproject.dto.ReviewDto;
import com.example.diplomproject.entity.User;
import com.example.diplomproject.service.CourseService;
import com.example.diplomproject.service.OrderService;
import com.example.diplomproject.service.ReviewService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Controller
@RequestMapping("/courses/{courseId}/reviews")
public class ReviewController {

    private final ReviewService reviewService;
    private final CourseService courseService;
    private final OrderService orderService;

    @Autowired
    public ReviewController(ReviewService reviewService,
                            CourseService courseService,
                            OrderService orderService) {
        this.reviewService = reviewService;
        this.courseService = courseService;
        this.orderService = orderService;
    }

    @GetMapping
    public String listReviews(@PathVariable Long courseId, Model model) {
        try {
            CourseDto courseDto = courseService.getCourseDtoById(courseId);
            List<ReviewDto> reviewDtos = reviewService.getReviewDtosByCourseId(courseId);
            Double averageRating = reviewService.averageRatingForCourse(courseId);

            model.addAttribute("course", courseDto);
            model.addAttribute("reviews", reviewDtos);
            model.addAttribute("averageRating", averageRating);
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", "Курс не найден");
            return "error/404";
        } catch (Exception e) {
            log.error("Error loading reviews for course {}", courseId, e);
            model.addAttribute("error", "Ошибка загрузки отзывов");
            return "error/500";
        }
        return "pages/reviews/list :: reviews-list-content";
    }

    @GetMapping("/new")
    public String showCreateForm(@PathVariable Long courseId,
                                 @AuthenticationPrincipal User currentUser,
                                 Model model) {
        if (currentUser == null) {
            return "redirect:/login";
        }
        try {
            CourseDto courseDto = courseService.getCourseDtoById(courseId);
            if (!orderService.hasUserPurchasedCourse(currentUser.getId(), courseId)) {
                return "redirect:/courses/" + courseId + "?error=" + encode("Вы не приобрели этот курс");
            }
            if (reviewService.hasUserReviewedCourse(currentUser.getId(), courseId)) {
                return "redirect:/courses/" + courseId + "?error=" + encode("Вы уже оставили отзыв на этот курс");
            }
            model.addAttribute("course", courseDto);
            model.addAttribute("reviewDto", new ReviewDto());
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", "Курс не найден");
            return "error/404";
        } catch (Exception e) {
            log.error("Error loading create review form for course {}", courseId, e);
            model.addAttribute("error", "Ошибка загрузки формы");
            return "error/500";
        }
        model.addAttribute("content", "pages/reviews/form :: review-form-content");
        return "layouts/main";
    }

    @PostMapping
    public String createReview(@PathVariable Long courseId,
                               @RequestParam String text,
                               @RequestParam Integer rating,
                               @AuthenticationPrincipal User currentUser,
                               Model model) {
        if (currentUser == null) {
            return "redirect:/login";
        }
        try {
            reviewService.createReview(currentUser.getId(), courseId, text, rating);
        } catch (IllegalArgumentException | SecurityException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("course", courseService.getCourseDtoById(courseId));
            model.addAttribute("reviewDto", new ReviewDto());
            model.addAttribute("reviewText", text);
            model.addAttribute("reviewRating", rating);
            model.addAttribute("content", "pages/reviews/form :: review-form-content");
            return "layouts/main";
        }
        return "redirect:/courses/" + courseId + "?success=" + encode("Отзыв успешно добавлен");
    }

    @GetMapping("/{reviewId}/edit")
    public String showEditForm(@PathVariable Long courseId,
                               @PathVariable Long reviewId,
                               @AuthenticationPrincipal User currentUser,
                               Model model) {
        if (currentUser == null) {
            return "redirect:/login";
        }
        try {
            ReviewDto reviewDto = reviewService.getReviewDtoById(reviewId);
            if (!reviewDto.getCourseId().equals(courseId)) {
                return "error/404";
            }
            if (!reviewDto.getUserId().equals(currentUser.getId())) {
                return "error/403";
            }
            model.addAttribute("reviewDto", reviewDto);
            model.addAttribute("courseId", courseId);
            model.addAttribute("reviewText", reviewDto.getText());
            model.addAttribute("reviewRating", reviewDto.getRating());
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", "Отзыв не найден");
            return "error/404";
        } catch (Exception e) {
            log.error("Error loading edit review form", e);
            model.addAttribute("error", "Ошибка загрузки формы");
            return "error/500";
        }
        model.addAttribute("content", "pages/reviews/form :: review-form-content");
        return "layouts/main";
    }

    @PostMapping("/{reviewId}")
    public String updateReview(@PathVariable Long courseId,
                               @PathVariable Long reviewId,
                               @RequestParam(required = false) String text,
                               @RequestParam(required = false) Integer rating,
                               @AuthenticationPrincipal User currentUser,
                               Model model) {
        if (currentUser == null) {
            return "redirect:/login";
        }
        try {
            reviewService.updateReview(reviewId, text, rating, currentUser.getId());
        } catch (IllegalArgumentException | SecurityException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("course", courseService.getCourseDtoById(courseId));
            model.addAttribute("reviewDto", reviewService.getReviewDtoById(reviewId));
            model.addAttribute("reviewText", text);
            model.addAttribute("reviewRating", rating);
            model.addAttribute("content", "pages/reviews/form :: review-form-content");
            return "layouts/main";
        }
        return "redirect:/courses/" + courseId + "?success=" + encode("Отзыв обновлён");
    }

    @PostMapping("/{reviewId}/delete")
    public String deleteReview(@PathVariable Long courseId,
                               @PathVariable Long reviewId,
                               @AuthenticationPrincipal User currentUser,
                               Model model) {
        if (currentUser == null) {
            return "redirect:/login";
        }
        try {
            reviewService.deleteReview(reviewId, currentUser.getId());
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", "Отзыв не найден");
            return "error/404";
        } catch (SecurityException e) {
            return "error/403";
        } catch (Exception e) {
            log.error("Error deleting review", e);
            return "redirect:/courses/" + courseId + "?error=" + encode("Ошибка удаления");
        }
        return "redirect:/courses/" + courseId + "?success=" + encode("Отзыв удалён");
    }

    private String encode(String message) {
        return URLEncoder.encode(message, StandardCharsets.UTF_8);
    }
}