package com.example.diplomproject.mapper;

import com.example.diplomproject.dto.ReviewDto;
import com.example.diplomproject.entity.Course;
import com.example.diplomproject.entity.Review;
import com.example.diplomproject.entity.User;
import com.example.diplomproject.enums.ModerationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class ReviewMapperTest {

    private ReviewMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ReviewMapper();
    }

    @Test
    void toReviewDTO_shouldMapFullReview() {
        // given
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");

        Course course = new Course();
        course.setId(10L);
        course.setTitle("Test Course");

        Review review = new Review();
        review.setId(100L);
        review.setUser(user);
        review.setCourse(course);
        review.setText("Great course!");
        review.setRating(5);
        review.setCreatedAt(LocalDateTime.of(2025, 1, 1, 12, 0));
        review.setModerationStatus(ModerationStatus.APPROVED);

        // when
        ReviewDto dto = mapper.toReviewDTO(review);

        // then
        assertNotNull(dto);
        assertEquals(100L, dto.getId());
        assertEquals(1L, dto.getUserId());
        assertEquals("testuser", dto.getUsername());
        assertEquals(10L, dto.getCourseId());
        assertEquals("Test Course", dto.getCourseTitle());
        assertEquals("Great course!", dto.getText());
        assertEquals(5, dto.getRating());
        assertEquals(LocalDateTime.of(2025, 1, 1, 12, 0), dto.getCreatedAt());
        assertEquals("APPROVED", dto.getModerationStatus());
    }

    @Test
    void toReviewDTO_shouldHandleNullRelations() {
        Review review = new Review();
        review.setId(200L);
        review.setUser(null);
        review.setCourse(null);
        review.setText("No user/course");
        review.setRating(3);
        review.setModerationStatus(ModerationStatus.PENDING);

        ReviewDto dto = mapper.toReviewDTO(review);

        assertNotNull(dto);
        assertNull(dto.getUserId());
        assertNull(dto.getUsername());
        assertNull(dto.getCourseId());
        assertNull(dto.getCourseTitle());
        assertEquals("No user/course", dto.getText());
        assertEquals(3, dto.getRating());
        assertEquals("PENDING", dto.getModerationStatus());
    }

    @Test
    void toNewEntity_shouldMapDtoToNewReview() {
        ReviewDto dto = new ReviewDto();
        dto.setText("New review");
        dto.setRating(4);
        dto.setModerationStatus("APPROVED");

        Review review = mapper.toNewEntity(dto);

        assertNotNull(review);
        assertNull(review.getId());
        assertNull(review.getUser());
        assertNull(review.getCourse());
        assertEquals("New review", review.getText());
        assertEquals(4, review.getRating());
        assertEquals(ModerationStatus.APPROVED, review.getModerationStatus());
        assertNull(review.getCreatedAt()); // должно установиться в @PrePersist
    }

    @Test
    void updateEntityFromDto_shouldUpdateFields() {
        Review review = new Review();
        review.setText("Old text");
        review.setRating(2);
        review.setModerationStatus(ModerationStatus.PENDING);

        ReviewDto dto = new ReviewDto();
        dto.setText("Updated text");
        dto.setRating(5);
        dto.setModerationStatus("APPROVED");

        mapper.updateEntityFromDto(review, dto);

        assertEquals("Updated text", review.getText());
        assertEquals(5, review.getRating());
        assertEquals(ModerationStatus.APPROVED, review.getModerationStatus());
    }

    @Test
    void updateEntityFromDto_shouldNotUpdateNullFields() {
        Review review = new Review();
        review.setText("Original");
        review.setRating(3);

        ReviewDto dto = new ReviewDto();
        dto.setText(null);
        dto.setRating(null);

        mapper.updateEntityFromDto(review, dto);

        assertEquals("Original", review.getText());
        assertEquals(3, review.getRating());
    }

    @Test
    void toReviewDTO_shouldReturnNullForNullInput() {
        assertNull(mapper.toReviewDTO(null));
    }
}