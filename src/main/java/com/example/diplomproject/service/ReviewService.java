package com.example.diplomproject.service;

import com.example.diplomproject.dto.ReviewDto;
import com.example.diplomproject.entity.Course;
import com.example.diplomproject.entity.Review;
import com.example.diplomproject.entity.User;
import com.example.diplomproject.mapper.ReviewMapper;
import com.example.diplomproject.repository.ReviewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final OrderService orderService;
    private final CourseService courseService;
    private final UserService userService;
    private final ReviewMapper reviewMapper;

    @Autowired
    public ReviewService(ReviewRepository reviewRepository,
                         OrderService orderService,
                         CourseService courseService,
                         UserService userService,
                         ReviewMapper reviewMapper) {
        this.reviewRepository = reviewRepository;
        this.orderService = orderService;
        this.courseService = courseService;
        this.userService = userService;
        this.reviewMapper = reviewMapper;
    }

    // ==================== Пользовательские методы (работают с DTO и ID) ====================

    @Transactional
    public void createReview(Long userId, Long courseId, String text, Integer rating) {
        User user = userService.getUserById(userId);
        Course course = courseService.getCourseEntityById(courseId);
        if (user == null) throw new IllegalArgumentException("Пользователь не найден");
        if (course == null) throw new IllegalArgumentException("Курс не найден");
        if (text == null || text.trim().isEmpty())
            throw new IllegalArgumentException("Содержание отзыва должно содержать текст");
        if (rating == null || rating < 1 || rating > 5)
            throw new IllegalArgumentException("Рейтинг должен быть от 1 до 5");

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
        reviewRepository.save(review);
    }

    public ReviewDto getReviewDtoById(Long id) {
        Review review = getReviewEntityById(id);
        return reviewMapper.toReviewDTO(review);
    }

    public List<ReviewDto> getReviewDtosByCourseId(Long courseId) {
        Course course = courseService.getCourseEntityById(courseId);
        List<Review> reviews = reviewRepository.findByCourseAndHiddenFalseOrderByCreatedAtDesc(course);
        return reviews.stream()
                .map(reviewMapper::toReviewDTO)
                .collect(Collectors.toList());
    }

    public Double averageRatingForCourse(Long courseId) {
        Course course = courseService.getCourseEntityById(courseId);
        List<Review> reviews = getReviewsByCourseEntity(course);
        if (reviews.isEmpty()) return 0.0;
        return reviews.stream().mapToInt(Review::getRating).average().orElse(0.0);
    }

    public boolean hasUserReviewedCourse(Long userId, Long courseId) {
        User user = userService.getUserById(userId);
        Course course = courseService.getCourseEntityById(courseId);
        return reviewRepository.existsByUserAndCourse(user, course);
    }

    @Transactional
    public void updateReview(Long reviewId, String text, Integer rating, Long currentUserId) {
        Review review = getReviewEntityById(reviewId);
        if (!review.getUser().getId().equals(currentUserId)) {
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
        reviewRepository.save(review);
    }

    @Transactional
    public void deleteReview(Long reviewId, Long currentUserId) {
        Review review = getReviewEntityById(reviewId);
        if (!review.getUser().getId().equals(currentUserId)) {
            throw new SecurityException("Вы не можете удалить чужой отзыв");
        }
        reviewRepository.delete(review);
    }

    // ==================== Вспомогательные методы (возвращают сущности для внутреннего использования) ====================

    public Review getReviewEntityById(Long id) {
        return reviewRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Отзыв не найден!"));
    }

    private List<Review> getReviewsByCourseEntity(Course course) {
        if (course == null) throw new IllegalArgumentException("Не указан курс");
        return reviewRepository.findByCourseAndHiddenFalseOrderByCreatedAtDesc(course);
    }

    // ==================== Административные методы (могут возвращать сущности или DTO) ====================

    public List<Review> getAllReviews() {
        return reviewRepository.findAll();
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
        Review review = getReviewEntityById(reviewId);
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
        Review review = getReviewEntityById(reviewId);
        reviewRepository.delete(review);
    }

    @Transactional
    public void hideReview(Long reviewId) {
        Review review = getReviewEntityById(reviewId);
        review.setHidden(true);
        reviewRepository.save(review);
    }

    @Transactional
    public void showReview(Long reviewId) {
        Review review = getReviewEntityById(reviewId);
        review.setHidden(false);
        reviewRepository.save(review);
    }

    public List<Review> findByCourseId(Long id) {
        return reviewRepository.findByCourseId(id);
    }
}