package com.example.diplomproject.controller;

import com.example.diplomproject.dto.CourseDto;
import com.example.diplomproject.dto.ReviewDto;
import com.example.diplomproject.entity.User;
import com.example.diplomproject.enums.Role;
import com.example.diplomproject.service.CourseService;
import com.example.diplomproject.service.OrderService;
import com.example.diplomproject.service.ReviewService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

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
    public String listReviews(@PathVariable Long courseId,
                              @AuthenticationPrincipal User currentUser,
                              Model model) {
        try {
            CourseDto courseDto = courseService.getCourseDtoById(courseId);
            List<ReviewDto> reviewDtos = reviewService.getVisibleReviewDtosByCourseId(courseId, currentUser);
            double averageRating = reviewService.averageRatingForCourse(courseId);

            boolean canReview = false;
            if (currentUser != null) {
                canReview = !reviewService.hasUserActiveReview(currentUser.getId(), courseId);
            }

            model.addAttribute("course", courseDto);
            model.addAttribute("reviews", reviewDtos);
            model.addAttribute("averageRating", averageRating);
            model.addAttribute("canReview", canReview);
            model.addAttribute("title", "Отзывы о курсе «" + courseDto.getTitle() + "»");
            model.addAttribute("content", "pages/reviews/list :: reviews-list-content");
            return "layouts/main";
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Курс не найден");
        } catch (Exception e) {
            log.error("Error loading reviews for course {}", courseId, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Ошибка загрузки отзывов");
        }
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
            if (!isAdmin(currentUser) && !orderService.hasUserPurchasedCourse(currentUser.getId(), courseId)) {
                return "redirect:/courses/" + courseId + "?error=" + encode("Вы не приобрели этот курс");
            }
            if (reviewService.hasUserReviewedCourse(currentUser.getId(), courseId)) {
                return "redirect:/courses/" + courseId + "?error=" + encode("Вы уже оставили отзыв на этот курс");
            }
            model.addAttribute("course", courseDto);
            model.addAttribute("reviewDto", new ReviewDto());
            model.addAttribute("title", "Написать отзыв");
            model.addAttribute("content", "pages/reviews/form :: review-form-content");
            return "layouts/main";
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Курс не найден");
        } catch (Exception e) {
            log.error("Error loading create review form for course {}", courseId, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Ошибка загрузки формы");
        }
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
        // Валидация рейтинга
        if (rating == null || rating < 1 || rating > 5) {
            return redirectWithError(courseId, "Рейтинг должен быть от 1 до 5");
        }
        if (text == null || text.trim().isEmpty()) {
            return redirectWithError(courseId, "Текст отзыва не может быть пустым");
        }

        try {
            reviewService.createReview(currentUser.getId(), courseId, text, rating);
        } catch (IllegalArgumentException | SecurityException e) {
            try {
                CourseDto courseDto = courseService.getCourseDtoById(courseId);
                model.addAttribute("course", courseDto);
                model.addAttribute("error", e.getMessage());
                ReviewDto reviewDto = new ReviewDto();
                reviewDto.setText(text);
                reviewDto.setRating(rating);
                model.addAttribute("reviewDto", reviewDto);
                model.addAttribute("title", "Написать отзыв");
                model.addAttribute("content", "pages/reviews/form :: review-form-content");
                return "layouts/main";
            } catch (IllegalArgumentException ex) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Курс не найден");
            }
        } catch (Exception e) {
            log.error("Unexpected error creating review", e);
            return "redirect:/courses/" + courseId + "?error=" + encode("Ошибка создания отзыва");
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
            CourseDto courseDto = courseService.getCourseDtoById(courseId);
            ReviewDto reviewDto = reviewService.getReviewDtoById(reviewId);
            if (!reviewDto.getCourseId().equals(courseId)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Отзыв не принадлежит этому курсу");
            }
            if (!reviewDto.getUserId().equals(currentUser.getId()) && !isAdmin(currentUser)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Нет прав на редактирование");
            }
            model.addAttribute("reviewDto", reviewDto);
            model.addAttribute("course", courseDto);
            model.addAttribute("title", "Редактирование отзыва");
            model.addAttribute("content", "pages/reviews/form :: review-form-content");
            return "layouts/main";
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Отзыв не найден");
        } catch (Exception e) {
            log.error("Error loading edit review form", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Ошибка загрузки формы");
        }
    }

    @PostMapping("/{reviewId}")
    public String updateReview(@PathVariable Long courseId,
                               @PathVariable Long reviewId,
                               @RequestParam String text,
                               @RequestParam Integer rating,
                               @AuthenticationPrincipal User currentUser,
                               Model model) {
        if (currentUser == null) {
            return "redirect:/login";
        }
        // Валидация
        if (rating == null || rating < 1 || rating > 5) {
            return redirectWithError(courseId, "Рейтинг должен быть от 1 до 5");
        }
        if (text == null || text.trim().isEmpty()) {
            return redirectWithError(courseId, "Текст отзыва не может быть пустым");
        }

        ReviewDto existing;
        try {
            existing = reviewService.getReviewDtoById(reviewId);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Отзыв не найден");
        }
        if (!existing.getCourseId().equals(courseId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Отзыв не принадлежит этому курсу");
        }
        if (!existing.getUserId().equals(currentUser.getId()) && !isAdmin(currentUser)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Нет прав на редактирование");
        }

        try {
            reviewService.updateReview(reviewId, text, rating, currentUser.getId());
        } catch (IllegalArgumentException | SecurityException e) {
            try {
                CourseDto courseDto = courseService.getCourseDtoById(courseId);
                model.addAttribute("course", courseDto);
                model.addAttribute("error", e.getMessage());
                model.addAttribute("reviewDto", existing);
                model.addAttribute("title", "Редактирование отзыва");
                model.addAttribute("content", "pages/reviews/form :: review-form-content");
                return "layouts/main";
            } catch (IllegalArgumentException ex) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Курс не найден");
            }
        } catch (Exception e) {
            log.error("Error updating review", e);
            return "redirect:/courses/" + courseId + "?error=" + encode("Ошибка обновления отзыва");
        }
        return "redirect:/courses/" + courseId + "?success=" + encode("Отзыв обновлён");
    }

    @PostMapping("/{reviewId}/delete")
    public String deleteReview(@PathVariable Long courseId,
                               @PathVariable Long reviewId,
                               @AuthenticationPrincipal User currentUser) {
        if (currentUser == null) {
            return "redirect:/login";
        }
        ReviewDto existing;
        try {
            existing = reviewService.getReviewDtoById(reviewId);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Отзыв не найден");
        }
        if (!existing.getCourseId().equals(courseId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Отзыв не принадлежит этому курсу");
        }
        if (!existing.getUserId().equals(currentUser.getId()) && !isAdmin(currentUser)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Нет прав на удаление");
        }

        try {
            reviewService.deleteReview(reviewId, currentUser.getId());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Отзыв не найден");
        } catch (SecurityException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
        } catch (Exception e) {
            log.error("Error deleting review", e);
            return "redirect:/courses/" + courseId + "?error=" + encode("Ошибка удаления отзыва");
        }
        return "redirect:/courses/" + courseId + "?success=" + encode("Отзыв удалён");
    }

    private String encode(String message) {
        return URLEncoder.encode(message, StandardCharsets.UTF_8);
    }

    private String redirectWithError(Long courseId, String errorMsg) {
        return "redirect:/courses/" + courseId + "?error=" + encode(errorMsg);
    }

    private boolean isAdmin(User user) {
        return user != null && user.getRole() == Role.ADMIN;
    }
}