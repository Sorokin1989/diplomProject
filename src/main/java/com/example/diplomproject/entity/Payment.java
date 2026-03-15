package com.example.diplomproject.entity;

import com.example.diplomproject.PaymentStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @Column(name = "total_sum", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalSum;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;


    @Column(name = "transaction_id", unique = true)
    private String transactionId;

    @Column(name = "gateway_transaction_id")
    private String gatewayTransactionId;

    @Column(name = "payment_gateway")
    private String paymentGateway;


    @Column(name = "created_at", updatable = false,nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at",nullable=false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt=createdAt;
        if (paymentStatus == null) {
            this.paymentStatus = PaymentStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }


}
