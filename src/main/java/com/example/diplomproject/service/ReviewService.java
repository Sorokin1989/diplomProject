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

    /**
     * Создание нового отзыва.
     * @param user     пользователь, оставляющий отзыв
     * @param course   курс, на который оставляется отзыв
     * @param text     текст отзыва
     * @param rating   оценка (1-5)
     * @return сохранённый отзыв
     */
    @Transactional
    public Review createReview(User user, Course course, String text, Integer rating) {
        // 1. Валидация входных данных
        if (user == null) {
            throw new IllegalArgumentException("Пользователь не может быть null");
        }
        if (course == null) {
            throw new IllegalArgumentException("Курс не может быть null");
        }
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Содержание отзыва должно содержать текст");
        }
        if (rating == null || rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Рейтинг должен быть от 1 до 5");
        }

        // 2. Проверка, что пользователь уже не оставлял отзыв на этот курс
        //    Используем метод existsByUserAndCourse, если он объявлен в репозитории.
        //    Если нет – следует добавить его: boolean existsByUserAndCourse(User user, Course course);
        if (reviewRepository.existsByUserAndCourse(user, course)) {
            throw new IllegalArgumentException("Вы уже оставили отзыв на этот курс");
        }

        // 3. Бизнес-правило: пользователь может оставить отзыв только после покупки курса.
        //    Для этого необходимо внедрить сервис заказов и проверить наличие завершённого заказа.
        //    В данном примере оставляем заглушку, но в реальном приложении следует реализовать.
         if (!orderService.hasUserPurchasedCourse(user, course)) {
             throw new SecurityException("Вы не можете оставить отзыв на курс, который не приобрели");
         }

        Review review = new Review();
        review.setUser(user);
        review.setCourse(course);
        review.setText(text.trim());
        review.setRating(rating);
        review.setCreatedAt(LocalDateTime.now());

        return reviewRepository.save(review);
    }

    /**
     * Получение отзыва по ID.
     * @param id идентификатор отзыва
     * @return найденный отзыв
     * @throws NoSuchElementException если отзыв не найден
     */
    public Review getReviewById(Long id) {
        return reviewRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Отзыв не найден!"));
    }

    /**
     * Получение всех отзывов курса.
     * @param course курс
     * @return список отзывов (может быть пустым)
     */
    public List<Review> getReviewsByCourse(Course course) {
        if (course == null) {
            throw new IllegalArgumentException("Не указан курс для получения списка отзывов.");
        }
        return reviewRepository.findByCourse(course);
    }

    /**
     * Обновление отзыва.
     * @param id          идентификатор отзыва
     * @param text        новый текст (если не null и не пустой)
     * @param rating      новая оценка (если не null, проверяется диапазон)
     * @param currentUser пользователь, выполняющий операцию (должен быть автором отзыва)
     * @return обновлённый отзыв
     * @throws SecurityException если пользователь не является автором
     */
    @Transactional
    public Review updateReview(Long id, String text, Integer rating, User currentUser) {
        Review review = getReviewById(id);

        // Проверка прав: только автор может редактировать
        if (!review.getUser().equals(currentUser)) {
            throw new SecurityException("Вы не можете редактировать чужой отзыв");
        }

        // Обновление текста
        if (text != null && !text.trim().isEmpty()) {
            review.setText(text.trim());
        }

        // Обновление рейтинга с валидацией
        if (rating != null) {
            if (rating < 1 || rating > 5) {
                throw new IllegalArgumentException("Рейтинг должен быть от 1 до 5");
            }
            review.setRating(rating);
        }

        // Явный save не обязателен, т.к. объект находится в persistence context,
        // но оставляем для наглядности.
        return reviewRepository.save(review);
    }

    /**
     * Удаление отзыва.
     * @param id          идентификатор отзыва
     * @param currentUser пользователь, выполняющий операцию (должен быть автором)
     * @throws SecurityException если пользователь не является автором
     */
    @Transactional
    public void deleteReview(Long id, User currentUser) {
        Review review = getReviewById(id);

        if (!review.getUser().equals(currentUser)) {
            throw new SecurityException("Вы не можете удалить чужой отзыв");
        }

        reviewRepository.delete(review);
    }

    /**
     * Получение среднего рейтинга курса.
     * @param course курс
     * @return средний рейтинг (0.0, если отзывов нет)
     */
    public double averageRateForCourse(Course course) {
        List<Review> reviews = getReviewsByCourse(course);
        if (reviews.isEmpty()) {
            return 0.0;
        }
        return reviews.stream()
                .mapToInt(Review::getRating)
                .average()
                .orElse(0.0);
    }
}