package com.example.diplomproject.service;

import com.example.diplomproject.dto.ReviewDto;
import com.example.diplomproject.entity.Course;
import com.example.diplomproject.entity.Review;
import com.example.diplomproject.entity.User;
import com.example.diplomproject.enums.ModerationStatus;
import com.example.diplomproject.enums.Role;
import com.example.diplomproject.mapper.ReviewMapper;
import com.example.diplomproject.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private OrderService orderService;
    @Mock
    private CourseService courseService;
    @Mock
    private UserService userService;
    @Mock
    private ReviewMapper reviewMapper;

    @InjectMocks
    private ReviewService reviewService;

    private User user;
    private User admin;
    private Course course;
    private Review review;
    private ReviewDto reviewDto;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("user");
        user.setRole(Role.USER);

        admin = new User();
        admin.setId(2L);
        admin.setUsername("admin");
        admin.setRole(Role.ADMIN);

        course = new Course();
        course.setId(10L);
        course.setTitle("Test Course");

        review = new Review();
        review.setId(100L);
        review.setUser(user);
        review.setCourse(course);
        review.setText("Great course!");
        review.setRating(5);
        review.setModerationStatus(ModerationStatus.PENDING);
        review.setHidden(false);
        review.setCreatedAt(LocalDateTime.now());

        reviewDto = new ReviewDto();
        reviewDto.setId(100L);
        reviewDto.setUserId(1L);
        reviewDto.setUsername("user");
        reviewDto.setCourseId(10L);
        reviewDto.setText("Great course!");
        reviewDto.setRating(5);
        reviewDto.setModerationStatus("PENDING");
    }

    // ========== createReview ==========
    @Test
    void createReview_shouldSaveWhenValid() {
        when(userService.getUserById(1L)).thenReturn(user);
        when(courseService.getCourseEntityById(10L)).thenReturn(course);
        when(orderService.hasUserPurchasedCourse(user, course)).thenReturn(true);
        when(reviewRepository.existsByUserAndCourseAndModerationStatusIn(any(), any(), any())).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenReturn(review);

        reviewService.createReview(1L, 10L, "Great course!", 5);

        verify(reviewRepository).save(any(Review.class));
    }

    @Test
    void createReview_shouldThrowWhenUserNotFound() {
        when(userService.getUserById(99L)).thenThrow(new IllegalArgumentException("Пользователь не найден"));
        assertThatThrownBy(() -> reviewService.createReview(99L, 10L, "text", 5))
                .isInstanceOf(IllegalArgumentException.class);
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void createReview_shouldThrowWhenCourseNotFound() {
        when(userService.getUserById(1L)).thenReturn(user);
        when(courseService.getCourseEntityById(99L)).thenThrow(new IllegalArgumentException("Курс не найден"));
        assertThatThrownBy(() -> reviewService.createReview(1L, 99L, "text", 5))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createReview_shouldThrowWhenAlreadyReviewed() {
        when(userService.getUserById(1L)).thenReturn(user);
        when(courseService.getCourseEntityById(10L)).thenReturn(course);
        when(reviewRepository.existsByUserAndCourseAndModerationStatusIn(any(), any(), any())).thenReturn(true);
        assertThatThrownBy(() -> reviewService.createReview(1L, 10L, "text", 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Вы уже оставили отзыв на этот курс (на модерации или одобрен)");
    }

    @Test
    void createReview_shouldThrowWhenNotPurchased() {
        when(userService.getUserById(1L)).thenReturn(user);
        when(courseService.getCourseEntityById(10L)).thenReturn(course);
        when(reviewRepository.existsByUserAndCourseAndModerationStatusIn(any(), any(), any())).thenReturn(false);
        when(orderService.hasUserPurchasedCourse(user, course)).thenReturn(false);
        assertThatThrownBy(() -> reviewService.createReview(1L, 10L, "text", 5))
                .isInstanceOf(SecurityException.class)
                .hasMessage("Вы не можете оставить отзыв на курс, который не приобрели");
    }

    // ========== getVisibleReviewDtosByCourseId ==========
    @Test
    void getVisibleReviewDtosByCourseId_shouldShowOnlyApprovedAndNotHiddenForGuest() {
        Review hiddenReview = new Review();
        hiddenReview.setId(200L);
        hiddenReview.setUser(user);
        hiddenReview.setCourse(course);
        hiddenReview.setRating(4);
        hiddenReview.setModerationStatus(ModerationStatus.APPROVED);
        hiddenReview.setHidden(true);
        Review approvedReview = new Review();
        approvedReview.setId(300L);
        approvedReview.setUser(user);
        approvedReview.setCourse(course);
        approvedReview.setRating(5);
        approvedReview.setModerationStatus(ModerationStatus.APPROVED);
        approvedReview.setHidden(false);
        Review pendingReview = new Review();
        pendingReview.setId(400L);
        pendingReview.setUser(user);
        pendingReview.setCourse(course);
        pendingReview.setRating(3);
        pendingReview.setModerationStatus(ModerationStatus.PENDING);
        pendingReview.setHidden(false);

        when(courseService.getCourseEntityById(10L)).thenReturn(course);
        when(reviewRepository.findByCourseOrderByCreatedAtDesc(course)).thenReturn(List.of(hiddenReview, approvedReview, pendingReview));
        when(reviewMapper.toReviewDTO(approvedReview)).thenReturn(new ReviewDto());

        List<ReviewDto> result = reviewService.getVisibleReviewDtosByCourseId(10L, null);

        assertThat(result).hasSize(1);
        verify(reviewMapper).toReviewDTO(approvedReview);
    }

    @Test
    void getVisibleReviewDtosByCourseId_shouldShowAllForAdmin() {
        when(courseService.getCourseEntityById(10L)).thenReturn(course);
        when(reviewRepository.findByCourseOrderByCreatedAtDesc(course)).thenReturn(List.of(review));
        when(reviewMapper.toReviewDTO(review)).thenReturn(reviewDto);

        List<ReviewDto> result = reviewService.getVisibleReviewDtosByCourseId(10L, admin);

        assertThat(result).hasSize(1);
        verify(reviewMapper).toReviewDTO(review);
    }

    @Test
    void getVisibleReviewDtosByCourseId_shouldShowOwnReviewsEvenIfHidden() {
        Review ownHiddenReview = new Review();
        ownHiddenReview.setId(200L);
        ownHiddenReview.setUser(user);
        ownHiddenReview.setCourse(course);
        ownHiddenReview.setRating(4);
        ownHiddenReview.setModerationStatus(ModerationStatus.APPROVED);
        ownHiddenReview.setHidden(true);

        when(courseService.getCourseEntityById(10L)).thenReturn(course);
        when(reviewRepository.findByCourseOrderByCreatedAtDesc(course)).thenReturn(List.of(ownHiddenReview));
        when(reviewMapper.toReviewDTO(ownHiddenReview)).thenReturn(new ReviewDto());

        List<ReviewDto> result = reviewService.getVisibleReviewDtosByCourseId(10L, user);

        assertThat(result).hasSize(1);
    }

    // ========== averageRatingForCourse ==========
    @Test
    void averageRatingForCourse_shouldCalculateAverage() {
        Review review1 = new Review();
        review1.setRating(4);
        Review review2 = new Review();
        review2.setRating(5);
        when(courseService.getCourseEntityById(10L)).thenReturn(course);
        when(reviewRepository.findByCourseAndModerationStatusAndHiddenFalse(course, ModerationStatus.APPROVED))
                .thenReturn(List.of(review1, review2));
        Double avg = reviewService.averageRatingForCourse(10L);
        assertThat(avg).isEqualTo(4.5);
    }

    // ========== updateReview ==========
    @Test
    void updateReview_shouldUpdateTextAndResetStatus() {
        when(reviewRepository.findById(100L)).thenReturn(Optional.of(review));
        review.setModerationStatus(ModerationStatus.APPROVED);
        reviewService.updateReview(100L, "Updated text", null, 1L);
        assertThat(review.getText()).isEqualTo("Updated text");
        assertThat(review.getModerationStatus()).isEqualTo(ModerationStatus.PENDING);
        verify(reviewRepository).save(review);
    }

    @Test
    void updateReview_shouldThrowWhenUserNotOwner() {
        when(reviewRepository.findById(100L)).thenReturn(Optional.of(review));
        assertThatThrownBy(() -> reviewService.updateReview(100L, "text", null, 99L))
                .isInstanceOf(SecurityException.class)
                .hasMessage("Вы не можете редактировать чужой отзыв");
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void updateReview_shouldThrowWhenReviewHidden() {
        review.setHidden(true);
        when(reviewRepository.findById(100L)).thenReturn(Optional.of(review));
        assertThatThrownBy(() -> reviewService.updateReview(100L, "text", null, 1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Этот отзыв скрыт администратором и не может быть изменён");
    }

    // ========== deleteReview ==========
    @Test
    void deleteReview_shouldDeleteWhenOwner() {
        when(reviewRepository.findById(100L)).thenReturn(Optional.of(review));
        reviewService.deleteReview(100L, 1L);
        verify(reviewRepository).delete(review);
    }

    @Test
    void deleteReview_shouldThrowWhenNotOwner() {
        when(reviewRepository.findById(100L)).thenReturn(Optional.of(review));
        assertThatThrownBy(() -> reviewService.deleteReview(100L, 99L))
                .isInstanceOf(SecurityException.class);
    }

    // ========== moderateReview ==========
    @Test
    void moderateReview_shouldChangeStatus() {
        when(reviewRepository.findById(100L)).thenReturn(Optional.of(review));
        reviewService.moderateReview(100L, ModerationStatus.APPROVED);
        assertThat(review.getModerationStatus()).isEqualTo(ModerationStatus.APPROVED);
        verify(reviewRepository).save(review);
    }

    // ========== hideReview / showReview ==========
    @Test
    void hideReview_shouldSetHiddenTrue() {
        when(reviewRepository.findById(100L)).thenReturn(Optional.of(review));
        reviewService.hideReview(100L);
        assertThat(review.isHidden()).isTrue();
        verify(reviewRepository).save(review);
    }

    @Test
    void showReview_shouldSetHiddenFalse() {
        review.setHidden(true);
        when(reviewRepository.findById(100L)).thenReturn(Optional.of(review));
        reviewService.showReview(100L);
        assertThat(review.isHidden()).isFalse();
        verify(reviewRepository).save(review);
    }

    // ========== getPendingReviewDtos ==========
    @Test
    void getPendingReviewDtos_shouldReturnOnlyPending() {
        when(reviewRepository.findByModerationStatus(ModerationStatus.PENDING)).thenReturn(List.of(review));
        when(reviewMapper.toReviewDTO(review)).thenReturn(reviewDto);
        List<ReviewDto> result = reviewService.getPendingReviewDtos();
        assertThat(result).hasSize(1);
    }
}