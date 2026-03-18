package com.example.diplomproject.mapper;

import com.example.diplomproject.dto.ReviewDto;
import com.example.diplomproject.entity.Review;
import org.springframework.stereotype.Component;

@Component
public class ReviewMapper {

    public ReviewDto toReviewDTO(Review review) {

        if (review == null)
            return null;

        ReviewDto reviewDto = new ReviewDto();

        reviewDto.setId(review.getId());

        if(review.getUser()!=null){
        reviewDto.setUserId(review.getUser().getId());
        reviewDto.setUsername(review.getUser().getUsername());
        }

        if (review.getCourse() !=null){
        reviewDto.setCourseId(review.getCourse().getId());
        reviewDto.setCourseTitle(review.getCourse().getTitle());
        }

        reviewDto.setText(review.getText());
        reviewDto.setRating(review.getRating());

        reviewDto.setCreatedAt(review.getCreatedAt());
        reviewDto.setModerationStatus(String.valueOf(review.getModerationStatus()));

        return  reviewDto;
    }

}
