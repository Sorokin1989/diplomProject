package com.example.diplomproject.controller;

import com.example.diplomproject.dto.CourseDto;
import com.example.diplomproject.dto.ReviewDto;
import com.example.diplomproject.entity.Course;
import com.example.diplomproject.entity.Review;
import com.example.diplomproject.entity.User;
import com.example.diplomproject.mapper.CourseMapper;
import com.example.diplomproject.mapper.ReviewMapper;
import com.example.diplomproject.service.CourseService;
import com.example.diplomproject.service.OrderService;
import com.example.diplomproject.service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/courses/{courseId}/reviews")
public class ReviewController {

    private final ReviewService reviewService;
    private final CourseService courseService;
    private final OrderService orderService;
    private final ReviewMapper reviewMapper;
    private final CourseMapper courseMapper;

    @Autowired
    public ReviewController(ReviewService reviewService,
                            CourseService courseService,
                            OrderService orderService,
                            ReviewMapper reviewMapper,
                            CourseMapper courseMapper) {
        this.reviewService = reviewService;
        this.courseService = courseService;
        this.orderService = orderService;
        this.reviewMapper = reviewMapper;
        this.courseMapper = courseMapper;
    }

    // Список отзывов для курса (фрагмент, встраиваемый в страницу курса)
    @GetMapping
    public String listReviews(@PathVariable Long courseId, Model model) {
        try {
            Course course = courseService.getCourseById(courseId);
            CourseDto courseDto = courseMapper.toCourseDto(course);
            List<Review> reviews = reviewService.getReviewsByCourse(course);
            List<ReviewDto> reviewDtos = reviews.stream()
                    .map(reviewMapper::toReviewDTO)
                    .collect(Collectors.toList());
            Double averageRating = reviewService.averageRateForCourse(course);

            model.addAttribute("course", courseDto);
            model.addAttribute("reviews", reviewDtos);
            model.addAttribute("averageRating", averageRating);
        } catch (NoSuchElementException e) {
            model.addAttribute("error", "Курс не найден");
            return "error/404";
        } catch (Exception e) {
            model.addAttribute("error", "Ошибка загрузки отзывов");
            return "error/500";
        }
        // Возвращаем только фрагмент (без общего layout)
        return "pages/reviews/list :: reviews-list-content";
    }

    // Форма добавления отзыва (доступна только купившим курс)
    @GetMapping("/new")
    public String showCreateForm(@PathVariable Long courseId,
                                 @AuthenticationPrincipal User currentUser,
                                 Model model) {
        if (currentUser == null) {
            return "redirect:/login";
        }
        try {
            Course course = courseService.getCourseById(courseId);
            if (!orderService.hasUserPurchasedCourse(currentUser, course)) {
                return "redirect:/courses/" + courseId + "?error=" + encode("Вы не приобрели этот курс");
            }
            if (reviewService.hasUserReviewedCourse(currentUser, course)) {
                return "redirect:/courses/" + courseId + "?error=" + encode("Вы уже оставили отзыв на этот курс");
            }
            model.addAttribute("course", courseMapper.toCourseDto(course));
            model.addAttribute("reviewDto", new ReviewDto());
        } catch (NoSuchElementException e) {
            model.addAttribute("error", "Курс не найден");
            return "error/404";
        } catch (Exception e) {
            model.addAttribute("error", "Ошибка загрузки формы");
            return "error/500";
        }
        model.addAttribute("content", "pages/reviews/form :: review-form-content");
        return "layouts/main";
    }

    // Обработка создания отзыва
    @PostMapping
    public String createReview(@PathVariable Long courseId,
                               @RequestParam String text,
                               @RequestParam Integer rating,
                               @AuthenticationPrincipal User currentUser,
                               Model model) {
        if (currentUser == null) {
            return "redirect:/login";
        }
        Course course;
        try {
            course = courseService.getCourseById(courseId);
        } catch (NoSuchElementException e) {
            model.addAttribute("error", "Курс не найден");
            return "error/404";
        }
        try {
            reviewService.createReview(currentUser, course, text, rating);
        } catch (IllegalArgumentException | SecurityException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("course", courseMapper.toCourseDto(course));
            model.addAttribute("reviewDto", new ReviewDto());
            model.addAttribute("reviewText", text);
            model.addAttribute("reviewRating", rating);
            model.addAttribute("content", "pages/reviews/form :: review-form-content");
            return "layouts/main";
        }
        return "redirect:/courses/" + courseId + "?success=" + encode("Отзыв успешно добавлен");
    }

    // Форма редактирования отзыва (только автор)
    @GetMapping("/{reviewId}/edit")
    public String showEditForm(@PathVariable Long courseId,
                               @PathVariable Long reviewId,
                               @AuthenticationPrincipal User currentUser,
                               Model model) {
        if (currentUser == null) {
            return "redirect:/login";
        }
        Review review;
        try {
            review = reviewService.getReviewById(reviewId);
            if (!review.getCourse().getId().equals(courseId)) {
                return "error/404";
            }
            if (!review.getUser().getId().equals(currentUser.getId())) {
                return "error/403";
            }
        } catch (NoSuchElementException e) {
            model.addAttribute("error", "Отзыв не найден");
            return "error/404";
        } catch (Exception e) {
            model.addAttribute("error", "Ошибка загрузки формы");
            return "error/500";
        }
        ReviewDto reviewDto = reviewMapper.toReviewDTO(review);
        model.addAttribute("reviewDto", reviewDto);
        model.addAttribute("courseId", courseId);
        model.addAttribute("reviewText", review.getText());
        model.addAttribute("reviewRating", review.getRating());
        model.addAttribute("content", "pages/reviews/form :: review-form-content");
        return "layouts/main";
    }

    // Обработка обновления отзыва
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
        Course course;
        Review existingReview;
        try {
            course = courseService.getCourseById(courseId);
            existingReview = reviewService.getReviewById(reviewId);
            if (!existingReview.getCourse().getId().equals(courseId)) {
                return "error/404";
            }
            if (!existingReview.getUser().getId().equals(currentUser.getId())) {
                return "error/403";
            }
        } catch (NoSuchElementException e) {
            model.addAttribute("error", "Курс или отзыв не найден");
            return "error/404";
        }
        try {
            reviewService.updateReview(reviewId, text, rating, currentUser);
        } catch (IllegalArgumentException | SecurityException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("course", courseMapper.toCourseDto(course));
            model.addAttribute("reviewDto", reviewMapper.toReviewDTO(existingReview));
            model.addAttribute("reviewText", text != null ? text : existingReview.getText());
            model.addAttribute("reviewRating", rating != null ? rating : existingReview.getRating());
            model.addAttribute("content", "pages/reviews/form :: review-form-content");
            return "layouts/main";
        }
        return "redirect:/courses/" + courseId + "?success=" + encode("Отзыв обновлён");
    }

    // Удаление отзыва (только автор)
    @PostMapping("/{reviewId}/delete")
    public String deleteReview(@PathVariable Long courseId,
                               @PathVariable Long reviewId,
                               @AuthenticationPrincipal User currentUser,
                               Model model) {
        if (currentUser == null) {
            return "redirect:/login";
        }
        try {
            Review review = reviewService.getReviewById(reviewId);
            if (!review.getCourse().getId().equals(courseId)) {
                return "error/404";
            }
            if (!review.getUser().getId().equals(currentUser.getId())) {
                return "error/403";
            }
            reviewService.deleteReview(reviewId, currentUser);
        } catch (NoSuchElementException e) {
            model.addAttribute("error", "Отзыв не найден");
            return "error/404";
        } catch (SecurityException e) {
            return "error/403";
        } catch (Exception e) {
            return "redirect:/courses/" + courseId + "?error=" + encode("Ошибка удаления");
        }
        return "redirect:/courses/" + courseId + "?success=" + encode("Отзыв удалён");
    }

    private String encode(String message) {
        return URLEncoder.encode(message, StandardCharsets.UTF_8);
    }
}