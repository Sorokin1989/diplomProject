package com.example.diplomproject.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class PaymentDto {

    private Long id;
    private Long orderId;
    private String orderStatus;
    private BigDecimal totalSum;
    private String paymentStatus;
    private String paymentMethod;
    private String transactionId;
    private String gatewayTransactionId;
    private String paymentGateway;
    private String currency="RUB";
    private String failureMessage;


    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
