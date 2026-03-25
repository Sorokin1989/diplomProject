package com.example.diplomproject.controller;

import com.example.diplomproject.entity.Course;
import com.example.diplomproject.entity.Review;
import com.example.diplomproject.entity.User;
import com.example.diplomproject.service.CourseService;
import com.example.diplomproject.service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/courses/{courseId}/reviews")
public class ReviewController {

    private final ReviewService reviewService;
    private final CourseService courseService;

    @Autowired
    public ReviewController(ReviewService reviewService, CourseService courseService) {
        this.reviewService = reviewService;
        this.courseService = courseService;
    }

    // Список отзывов для курса (обычно выводится на странице курса)
    @GetMapping
    public String listReviews(@PathVariable Long courseId, Model model) {
        Course course = courseService.getCourseById(courseId);
        model.addAttribute("course", course);
        model.addAttribute("reviews", reviewService.getReviewsByCourse(course));
        model.addAttribute("averageRating", reviewService.averageRateForCourse(course));
        return "pages/reviews/list"; // например, фрагмент, встроенный в страницу курса
    }

    // Форма добавления отзыва (доступна только тем, кто купил курс)
    @GetMapping("/new")
    public String showCreateForm(@PathVariable Long courseId,
                                 @AuthenticationPrincipal User currentUser,
                                 Model model) {
        Course course = courseService.getCourseById(courseId);
        // Проверка, что пользователь купил курс (иначе не может оставить отзыв)
        // Можно выбросить исключение или вернуть ошибку
        model.addAttribute("course", course);
        model.addAttribute("review", new Review());
        return "pages/reviews/form";
    }

    // Обработка создания отзыва
    @PostMapping
    public String createReview(@PathVariable Long courseId,
                               @RequestParam String text,
                               @RequestParam Integer rating,
                               @AuthenticationPrincipal User currentUser) {
        Course course = courseService.getCourseById(courseId);
        reviewService.createReview(currentUser, course, text, rating);
        // После добавления отзыва перенаправляем на страницу курса
        return "redirect:/courses/" + courseId;
    }

    // Форма редактирования отзыва (доступна только автору)
    @GetMapping("/{reviewId}/edit")
    public String showEditForm(@PathVariable Long courseId,
                               @PathVariable Long reviewId,
                               @AuthenticationPrincipal User currentUser,
                               Model model) {
        Review review = reviewService.getReviewById(reviewId);
        // Проверка прав: только автор может редактировать
        if (!review.getUser().getId().equals(currentUser.getId())) {
            return "error/403";
        }
        model.addAttribute("review", review);
        model.addAttribute("courseId", courseId);
        return "pages/reviews/form";
    }

    // Обработка обновления отзыва
    @PostMapping("/{reviewId}")
    public String updateReview(@PathVariable Long courseId,
                               @PathVariable Long reviewId,
                               @RequestParam(required = false) String text,
                               @RequestParam(required = false) Integer rating,
                               @AuthenticationPrincipal User currentUser) {
        reviewService.updateReview(reviewId, text, rating, currentUser);
        return "redirect:/courses/" + courseId;
    }

    // Удаление отзыва (только автор)
    @PostMapping("/{reviewId}/delete")
    public String deleteReview(@PathVariable Long courseId,
                               @PathVariable Long reviewId,
                               @AuthenticationPrincipal User currentUser) {
        reviewService.deleteReview(reviewId, currentUser);
        return "redirect:/courses/" + courseId;
    }
}