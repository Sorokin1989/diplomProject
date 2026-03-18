package com.example.diplomproject.entity;

import com.example.diplomproject.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable=false)
    private User user;

    @Column(name = "created_at", nullable=false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at",nullable = false)
    private LocalDateTime updatedAt;

    @Column(precision = 10, scale =2)
    private BigDecimal totalSum;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus orderStatus=OrderStatus.PENDING;


    @ToString.Exclude
    @OneToMany(mappedBy="order",
            fetch = FetchType.LAZY,
            cascade=CascadeType.ALL,
            orphanRemoval=true)
    private List<OrderItem> orderItems=new ArrayList<>();

    @ToString.Exclude
    @OneToOne(mappedBy ="order",fetch = FetchType.LAZY,cascade = CascadeType.ALL)
    private Payment payment;

    @OneToMany(mappedBy = "order",fetch = FetchType.LAZY)
    private List<CourseAccess>courseAccesses=new ArrayList<>();

    @Column(name = "discount_amount",nullable = false)
    private BigDecimal discountAmount;

    @ManyToOne(fetch = FetchType.LAZY,optional = false)
    @JoinColumn(name = "promocode_id",nullable = false)
    private Promocode promoCode;



    @PrePersist
    protected void onCreate() {
        createdAt =LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate(){
        updatedAt=LocalDateTime.now ();
    }

}
