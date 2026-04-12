package com.example.diplomproject.dto;


import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class CartItemDto {
    private Long id;
    private Long courseId;
    private String courseTitle;
    private BigDecimal price;
    private String imageUrl; // URL главного изображения курса

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime addedAt;
}
