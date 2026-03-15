package com.example.diplomproject.dto;

import com.example.diplomproject.entity.OrderItem;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
public class OrderDto {

    private Long id;
    private Long userId;
    private String username;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    private BigDecimal totalSum;
    private String orderStatus;
    private List<OrderItemDto> orderItemDtos;
    private List<CourseAccessDto> courseAccessDtos;
    private Long paymentId;
    private String paymentType;
    private String paymentStatus;
    private BigDecimal discountAmount;
    private String promoCode;



}
