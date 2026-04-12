package com.example.diplomproject.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class CourseDto {

    private Long id;
    private String title;
    private String description;
    private BigDecimal price;
    private String author;
    private Long categoryId;

    // Для обратной совместимости – URL главного изображения
    private String imageUrl;

    // Список URL всех изображений
    private List<String> imageUrls = new ArrayList<>();

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    private List<ReviewDto> reviewDtos;
    private Integer reviewCount;
    private String materialsPath;
}