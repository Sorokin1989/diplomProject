package com.example.diplomproject.service;

import com.example.diplomproject.dto.ReviewDto;
import com.example.diplomproject.entity.Course;
import com.example.diplomproject.entity.Review;
import com.example.diplomproject.entity.User;
import com.example.diplomproject.enums.ModerationStatus;
import com.example.diplomproject.enums.Role;
import com.example.diplomproject.mapper.ReviewMapper;
import com.example.diplomproject.repository.ReviewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Comparator;
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

    // ==================== Публичные методы (для пользователей и контроллеров) ====================

    /**
     * Создание нового отзыва – всегда со статусом PENDING (на модерации)
     */
    @Transactional
    public void createReview(Long userId, Long courseId, String text, Integer rating) {
        User user = userService.getUserById(userId);
        if (user == null) throw new IllegalArgumentException("Пользователь не найден");

        Course course = courseService.getCourseEntityById(courseId);
        if (course == null) throw new IllegalArgumentException("Курс не найден");

        if (text == null || text.trim().isEmpty())
            throw new IllegalArgumentException("Содержание отзыва не может быть пустым");
        if (rating == null || rating < 1 || rating > 5)
            throw new IllegalArgumentException("Рейтинг должен быть от 1 до 5");

        // Проверка: есть ли уже активный отзыв (PENDING или APPROVED)
        if (hasUserActiveReview(userId, courseId)) {
            throw new IllegalArgumentException("Вы уже оставили отзыв на этот курс (на модерации или одобрен)");
        }

        // Проверка покупки курса
        if (!orderService.hasUserPurchasedCourse(user, course)) {
            throw new SecurityException("Вы не можете оставить отзыв на курс, который не приобрели");
        }

        Review review = new Review();
        review.setUser(user);
        review.setCourse(course);
        review.setText(text.trim());
        review.setRating(rating);
        // createdAt устанавливается автоматически через @PrePersist в сущности
        review.setHidden(false);
        review.setModerationStatus(ModerationStatus.PENDING);
        reviewRepository.save(review);
    }

    /**
     * Получить DTO отзыва по ID
     */
    @Transactional(readOnly = true)
    public ReviewDto getReviewDtoById(Long id) {
        Review review = getReviewEntityById(id);
        return reviewMapper.toReviewDTO(review);
    }

    /**
     * Получить список ВИДИМЫХ отзывов для страницы курса (с учётом прав пользователя)
     * @param courseId ID курса
     * @param currentUser текущий пользователь (может быть null для неавторизованных)
     * @return список отзывов, которые должен видеть пользователь
     */
    @Transactional(readOnly = true)
    public List<ReviewDto> getVisibleReviewDtosByCourseId(Long courseId, User currentUser) {
        Course course = courseService.getCourseEntityById(courseId);
        List<Review> all = reviewRepository.findByCourseOrderByCreatedAtDesc(course);
        // all никогда не null (репозиторий возвращает пустой список, если нет записей)

        return all.stream()
                .filter(review -> {
                    // Администратор видит все отзывы (включая скрытые через hidden)
                    if (currentUser != null && currentUser.getRole() == Role.ADMIN) return true;
                    // Автор видит свои отзывы (любого статуса, даже если hidden=true)
                    if (currentUser != null && review.getUser().getId().equals(currentUser.getId())) return true;
                    // Обычные пользователи видят только одобренные и не скрытые
                    return review.getModerationStatus() == ModerationStatus.APPROVED && !review.isHidden();
                })
                .map(reviewMapper::toReviewDTO)
                .collect(Collectors.toList());
    }

    /**
     * Средний рейтинг курса – только по одобренным (APPROVED) и не скрытым отзывам
     */
    @Transactional(readOnly = true)
    public Double averageRatingForCourse(Long courseId) {
        Course course = courseService.getCourseEntityById(courseId);
        List<Review> approved = reviewRepository.findByCourseAndModerationStatusAndHiddenFalse(course, ModerationStatus.APPROVED);
        if (approved.isEmpty()) return 0.0;
        return approved.stream().mapToInt(Review::getRating).average().orElse(0.0);
    }

    /**
     * Количество одобренных отзывов для курса
     */
    @Transactional(readOnly = true)
    public int getApprovedReviewCount(Long courseId) {
        Course course = courseService.getCourseEntityById(courseId);
        return reviewRepository.countByCourseAndModerationStatusAndHiddenFalse(course, ModerationStatus.APPROVED);
    }

    /**
     * Проверка: есть ли у пользователя активный отзыв на курс (PENDING или APPROVED)
     */
    @Transactional(readOnly = true)
    public boolean hasUserActiveReview(Long userId, Long courseId) {
        User user = userService.getUserById(userId);
        Course course = courseService.getCourseEntityById(courseId);
        return reviewRepository.existsByUserAndCourseAndModerationStatusIn(user, course,
                List.of(ModerationStatus.PENDING, ModerationStatus.APPROVED));
    }

    /**
     * Проверка: оставлял ли пользователь когда-либо отзыв (любой статус)
     */
    @Transactional(readOnly = true)
    public boolean hasUserReviewedCourse(Long userId, Long courseId) {
        User user = userService.getUserById(userId);
        Course course = courseService.getCourseEntityById(courseId);
        return reviewRepository.existsByUserAndCourse(user, course);
    }

    /**
     * Редактирование отзыва пользователем – сбрасывает статус на PENDING, если отзыв был APPROVED
     */
    @Transactional
    public void updateReview(Long reviewId, String text, Integer rating, Long currentUserId) {
        Review review = getReviewEntityById(reviewId);
        if (!review.getUser().getId().equals(currentUserId)) {
            throw new SecurityException("Вы не можете редактировать чужой отзыв");
        }
        if (review.isHidden()) {
            throw new IllegalStateException("Этот отзыв скрыт администратором и не может быть изменён");
        }
        if ((text == null || text.trim().isEmpty()) && rating == null) {
            throw new IllegalArgumentException("Не указаны данные для обновления");
        }
        boolean changed = false;
        if (text != null && !text.trim().isEmpty()) {
            review.setText(text.trim());
            changed = true;
        }
        if (rating != null) {
            if (rating < 1 || rating > 5) throw new IllegalArgumentException("Рейтинг должен быть от 1 до 5");
            review.setRating(rating);
            changed = true;
        }
        if (changed && review.getModerationStatus() == ModerationStatus.APPROVED) {
            review.setModerationStatus(ModerationStatus.PENDING);
        }
        reviewRepository.save(review);
    }

    /**
     * Удаление отзыва пользователем
     */
    @Transactional
    public void deleteReview(Long reviewId, Long currentUserId) {
        Review review = getReviewEntityById(reviewId);
        if (!review.getUser().getId().equals(currentUserId)) {
            throw new SecurityException("Вы не можете удалить чужой отзыв");
        }
        if (review.isHidden()) {
            throw new IllegalStateException("Этот отзыв скрыт администратором и не может быть удалён");
        }
        reviewRepository.delete(review);
    }

    // ==================== Методы для администраторов ====================

    /**
     * Получить все отзывы (для админ‑панели)
     */
    @Transactional(readOnly = true)
    public List<ReviewDto> getAllReviewDtos() {
        return reviewRepository.findAll().stream()
                .map(reviewMapper::toReviewDTO)
                .collect(Collectors.toList());
    }

    /**
     * Получить отзывы, ожидающие модерации (PENDING)
     */
    @Transactional(readOnly = true)
    public List<ReviewDto> getPendingReviewDtos() {
        return reviewRepository.findByModerationStatus(ModerationStatus.PENDING)
                .stream().map(reviewMapper::toReviewDTO)
                .collect(Collectors.toList());
    }

    /**
     * Получить все отзывы курса (без фильтрации) – ТОЛЬКО ДЛЯ АДМИНИСТРАТОРА
     */
    @Transactional(readOnly = true)
    public List<ReviewDto> getAllReviewsByCourseIdForAdmin(Long courseId) {
        Course course = courseService.getCourseEntityById(courseId);
        return reviewRepository.findByCourseOrderByCreatedAtDesc(course)
                .stream()
                .map(reviewMapper::toReviewDTO)
                .collect(Collectors.toList());
    }

    /**
     * Изменить статус модерации отзыва (админ)
     */
    @Transactional
    public void moderateReview(Long reviewId, ModerationStatus newStatus) {
        Review review = getReviewEntityById(reviewId);
        review.setModerationStatus(newStatus);
        reviewRepository.save(review);
    }

    /**
     * Скрыть отзыв от всех пользователей (админ)
     */
    @Transactional
    public void hideReview(Long reviewId) {
        Review review = getReviewEntityById(reviewId);
        review.setHidden(true);
        reviewRepository.save(review);
    }

    /**
     * Показать скрытый отзыв (админ)
     */
    @Transactional
    public void showReview(Long reviewId) {
        Review review = getReviewEntityById(reviewId);
        review.setHidden(false);
        reviewRepository.save(review);
    }

    /**
     * Редактировать отзыв от имени администратора (без изменения статуса модерации)
     */
    @Transactional
    public void updateReviewByAdmin(Long reviewId, String text, Integer rating) {
        Review review = getReviewEntityById(reviewId);
        if (text != null && !text.trim().isEmpty()) {
            review.setText(text.trim());
        }
        if (rating != null) {
            if (rating < 1 || rating > 5) throw new IllegalArgumentException("Рейтинг должен быть от 1 до 5");
            review.setRating(rating);
        }
        reviewRepository.save(review);
    }

    /**
     * Полное удаление отзыва (админ)
     */
    @Transactional
    public void deleteReviewById(Long reviewId) {
        Review review = getReviewEntityById(reviewId);
        reviewRepository.delete(review);
    }

    // ==================== Вспомогательные методы ====================

    @Transactional(readOnly = true)
    public Review getReviewEntityById(Long id) {
        return reviewRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Отзыв не найден!"));
    }

    @Transactional(readOnly = true)
    public List<Review> getReviewsByCourseId(Long courseId) {
        return reviewRepository.findByCourseId(courseId);
    }

    @Transactional(readOnly = true)
    public List<Review> getReviewsByUserId(Long userId) {
        return reviewRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public List<Review> getReviewsWithMinRating(Integer minRating) {
        return reviewRepository.findByRatingGreaterThanEqual(minRating);
    }

    @Transactional(readOnly = true)
    public List<Review> getAllReviews() {
        return reviewRepository.findAll();
    }

    public List<Review> filterReviews(Long courseId, Long userId, Integer minRating, String modStatus) {
        // Используем спецификации или динамические запросы.
        // Для простоты – соберём все отзывы и отфильтруем в памяти (неэффективно при большом количестве).
        // Лучше реализовать через Criteria API или QueryDSL, но для демонстрации:
        List<Review> all = reviewRepository.findAll();
        return all.stream()
                .filter(r -> courseId == null || r.getCourse().getId().equals(courseId))
                .filter(r -> userId == null || r.getUser().getId().equals(userId))
                .filter(r -> minRating == null || r.getRating() >= minRating)
                .filter(r -> modStatus == null || modStatus.isEmpty() || r.getModerationStatus().name().equals(modStatus))
                .sorted(Comparator.comparing(Review::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }
}