package com.example.diplomproject.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class OrderItemDto {

    private Long id;
    private Long courseId;
    private String courseTitle;
    private BigDecimal price;
}
