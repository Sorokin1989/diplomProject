package com.example.diplomproject.entity;

import com.example.diplomproject.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(precision = 10, scale = 2)
    private BigDecimal totalSum = BigDecimal.ZERO;

    @Column(name = "hidden", nullable = false)
    private boolean hidden = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus orderStatus = OrderStatus.PENDING;

    @ToString.Exclude
    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();

    @ToString.Exclude
    @OneToOne(mappedBy = "order", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Payment payment;

    @ToString.Exclude
    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CourseAccess> courseAccesses = new ArrayList<>();

    @Column(name = "discount_amount")
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promocode_id")
    private Promocode promoCode;

    // ========== Вспомогательные методы для управления связями ==========

    public void addOrderItem(OrderItem orderItem) {
        orderItems.add(orderItem);
        orderItem.setOrder(this);
    }

    public void removeOrderItem(OrderItem orderItem) {
        orderItems.remove(orderItem);
        orderItem.setOrder(null);
    }

    public void addCourseAccess(CourseAccess courseAccess) {
        courseAccesses.add(courseAccess);
        courseAccess.setOrder(this);
    }

    public void removeCourseAccess(CourseAccess courseAccess) {
        courseAccesses.remove(courseAccess);
        courseAccess.setOrder(null);
    }

    /**
     * Пересчёт общей суммы заказа на основе позиций (с учётом скидки)
     */
    public void recalculateTotalSum() {
        BigDecimal itemsTotal = orderItems.stream()
                .map(OrderItem::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.totalSum = itemsTotal.subtract(discountAmount != null ? discountAmount : BigDecimal.ZERO);
        if (this.totalSum.compareTo(BigDecimal.ZERO) < 0) {
            this.totalSum = BigDecimal.ZERO;
        }
    }
}