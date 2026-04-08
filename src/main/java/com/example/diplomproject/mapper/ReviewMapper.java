package com.example.diplomproject.mapper;

import com.example.diplomproject.dto.ReviewDto;
import com.example.diplomproject.entity.Review;
import com.example.diplomproject.enums.ModerationStatus;
import org.springframework.stereotype.Component;

@Component
public class ReviewMapper {

    public ReviewDto toReviewDTO(Review review) {
        if (review == null) return null;

        ReviewDto reviewDto = new ReviewDto();
        reviewDto.setId(review.getId());

        if (review.getUser() != null) {
            reviewDto.setUserId(review.getUser().getId());
            reviewDto.setUsername(review.getUser().getUsername());
        }

        if (review.getCourse() != null) {
            reviewDto.setCourseId(review.getCourse().getId());
            reviewDto.setCourseTitle(review.getCourse().getTitle());
        }

        reviewDto.setText(review.getText());
        reviewDto.setRating(review.getRating());
        reviewDto.setCreatedAt(review.getCreatedAt());
        reviewDto.setModerationStatus(String.valueOf(review.getModerationStatus()));

        // Если в сущности есть updatedAt, добавьте:
        // reviewDto.setUpdatedAt(review.getUpdatedAt());

        return reviewDto;
    }

    /**
     * Преобразование DTO в новую сущность (только для создания).
     * Поле createdAt НЕ копируется – оно установится автоматически.
     */
    public Review toNewEntity(ReviewDto reviewDto) {
        if (reviewDto == null) return null;

        Review review = new Review();
        review.setText(reviewDto.getText());
        review.setRating(reviewDto.getRating());
        if (reviewDto.getModerationStatus() != null) {
            review.setModerationStatus(ModerationStatus.valueOf(reviewDto.getModerationStatus()));
        }
        // Связи (user, course) устанавливаются отдельно в сервисе
        return review;
    }

    /**
     * Обновление существующей сущности из DTO (для редактирования).
     * Не трогает id, createdAt, связи.
     */
    public void updateEntityFromDto(Review review, ReviewDto reviewDto) {
        if (reviewDto == null) return;
        if (reviewDto.getText() != null) {
            review.setText(reviewDto.getText());
        }
        if (reviewDto.getRating() != null) {
            review.setRating(reviewDto.getRating());
        }
        if (reviewDto.getModerationStatus() != null) {
            review.setModerationStatus(ModerationStatus.valueOf(reviewDto.getModerationStatus()));
        }
        // updatedAt обычно обновляется автоматически через @UpdateTimestamp
    }
}