package com.example.diplomproject.dto;

import com.example.diplomproject.entity.Review;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
public class CourseDto {

    private Long id;
    private String title;
    private String description;
    private BigDecimal price;
    private String author;
    private String categoryTitle;
    private String imageUrl;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    private List<ReviewDto>reviewDtos;
    private Integer reviewCount;
}
