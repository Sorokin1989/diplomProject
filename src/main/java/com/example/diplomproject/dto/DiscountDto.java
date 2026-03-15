package com.example.diplomproject.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
public class DiscountDto {
    private Long id;
    private String title;
    private String description;
    private String discountType;
    private BigDecimal discountValue;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startDate;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endDate;

    private List<Long> applicableCourseIds;
    private List<String> applicableCourseTitles;
    private List<Long> applicableCategoryIds;
    private List<String> applicableCategoryTitles;
    private BigDecimal minOrderAmount;
    private boolean active;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}