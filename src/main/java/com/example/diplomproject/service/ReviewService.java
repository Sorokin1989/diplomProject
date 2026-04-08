package com.example.diplomproject.service;

import com.example.diplomproject.entity.Course;
import com.example.diplomproject.entity.Review;
import com.example.diplomproject.entity.User;
import com.example.diplomproject.repository.ReviewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final OrderService orderService;

    @Autowired
    public ReviewService(ReviewRepository reviewRepository, OrderService orderService) {
        this.reviewRepository = reviewRepository;
        this.orderService = orderService;
    }

    @Transactional
    public Review createReview(User user, Course course, String text, Integer rating) {
        if (user == null) throw new IllegalArgumentException("Пользователь не может быть null");
        if (course == null) throw new IllegalArgumentException("Курс не может быть null");
        if (text == null || text.trim().isEmpty()) throw new IllegalArgumentException("Содержание отзыва должно содержать текст");
        if (rating == null || rating < 1 || rating > 5) throw new IllegalArgumentException("Рейтинг должен быть от 1 до 5");

        if (reviewRepository.existsByUserAndCourse(user, course)) {
            throw new IllegalArgumentException("Вы уже оставили отзыв на этот курс");
        }

        if (!orderService.hasUserPurchasedCourse(user, course)) {
            throw new SecurityException("Вы не можете оставить отзыв на курс, который не приобрели");
        }

        Review review = new Review();
        review.setUser(user);
        review.setCourse(course);
        review.setText(text.trim());
        review.setRating(rating);
        review.setCreatedAt(LocalDateTime.now());
        review.setHidden(false);

        return reviewRepository.save(review);
    }

    public Review getReviewById(Long id) {
        return reviewRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Отзыв не найден!"));
    }

    public List<Review> getReviewsByCourse(Course course) {
        if (course == null) throw new IllegalArgumentException("Не указан курс");
        // Используем метод с сортировкой
        return reviewRepository.findByCourseAndHiddenFalseOrderByCreatedAtDesc(course);
    }

    @Transactional
    public Review updateReview(Long id, String text, Integer rating, User currentUser) {
        Review review = getReviewById(id);
        if (!review.getUser().equals(currentUser)) {
            throw new SecurityException("Вы не можете редактировать чужой отзыв");
        }

        if ((text == null || text.trim().isEmpty()) && rating == null) {
            throw new IllegalArgumentException("Не указаны данные для обновления");
        }

        if (text != null && !text.trim().isEmpty()) {
            review.setText(text.trim());
        }
        if (rating != null) {
            if (rating < 1 || rating > 5) throw new IllegalArgumentException("Рейтинг должен быть от 1 до 5");
            review.setRating(rating);
        }
        return reviewRepository.save(review);
    }

    @Transactional
    public void deleteReview(Long id, User currentUser) {
        Review review = getReviewById(id);
        if (!review.getUser().equals(currentUser)) {
            throw new SecurityException("Вы не можете удалить чужой отзыв");
        }
        reviewRepository.delete(review);
    }

    public double averageRateForCourse(Course course) {
        List<Review> reviews = getReviewsByCourse(course);
        if (reviews.isEmpty()) return 0.0;
        return reviews.stream().mapToInt(Review::getRating).average().orElse(0.0);
    }

    public boolean hasUserReviewedCourse(User user, Course course) {
        return reviewRepository.existsByUserAndCourse(user, course);
    }

    // ========== Административные методы ==========

    public List<Review> getAllReviews() {
        // Сортировка через findAll с Sort (если нужна, можно оставить без сортировки)
        return reviewRepository.findAll(); // при желании добавьте сортировку
    }

    public List<Review> getReviewsByCourseId(Long courseId) {
        return reviewRepository.findByCourseIdOrderByCreatedAtDesc(courseId);
    }

    public List<Review> getReviewsByUserId(Long userId) {
        return reviewRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<Review> getReviewsWithMinRating(Integer minRating) {
        return reviewRepository.findByRatingGreaterThanEqualOrderByCreatedAtDesc(minRating);
    }

    @Transactional
    public Review updateReviewByAdmin(Long reviewId, String text, Integer rating) {
        Review review = getReviewById(reviewId);
        if ((text == null || text.trim().isEmpty()) && rating == null) {
            throw new IllegalArgumentException("Не указаны данные для обновления");
        }
        if (text != null && !text.trim().isEmpty()) {
            review.setText(text.trim());
        }
        if (rating != null) {
            if (rating < 1 || rating > 5) throw new IllegalArgumentException("Рейтинг должен быть от 1 до 5");
            review.setRating(rating);
        }
        return reviewRepository.save(review);
    }

    @Transactional
    public void deleteReviewById(Long reviewId) {
        Review review = getReviewById(reviewId);
        reviewRepository.delete(review);
    }

    @Transactional
    public void hideReview(Long reviewId) {
        Review review = getReviewById(reviewId);
        review.setHidden(true);
        reviewRepository.save(review);
    }

    @Transactional
    public void showReview(Long reviewId) {
        Review review = getReviewById(reviewId);
        review.setHidden(false);
        reviewRepository.save(review);
    }

    public List<Review> findByCourseId(Long id) {
        return reviewRepository.findByCourseId(id);
    }

//    public List<Review> getReviewsByCourseId(Long courseId) {
//        return reviewRepository.findByCourseId(courseId);
//    }


}