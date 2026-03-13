package com.example.diplomproject.entity;

import com.example.diplomproject.OrderStatus;
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
    private LocalDateTime date;

    @Column(precision = 10, scale =2)
    private BigDecimal totalSum;

    @Column
    @Enumerated(EnumType.STRING)
    private OrderStatus status=OrderStatus.PENDING;


    @ToString.Exclude
    @OneToMany(mappedBy="order",
            fetch = FetchType.LAZY,
            cascade=CascadeType.ALL,
            orphanRemoval=true)
    private List<OrderItem> orderItems=new ArrayList<>();

    @ToString.Exclude
    @OneToOne(mappedBy ="order",fetch = FetchType.LAZY,cascade = CascadeType.ALL)
    private Payment payment;



    @PrePersist
    protected void onCreate() {
        date =LocalDateTime.now();
    }

}
