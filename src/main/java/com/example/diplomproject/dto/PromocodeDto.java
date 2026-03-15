package com.example.diplomproject.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
public class PromocodeDto {
    private Long id;
    private String code;
    private String discountType;
    private BigDecimal value;
    private BigDecimal minOrderAmount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime validFrom;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime validTo;
    private Integer usageLimit;
    private Integer usedCount;
    private List<CourseDto> applicableCourseDtos;

    private List<CategoryDto> applicableCategoryDtos;
    private boolean active;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

}
