package com.example.diplomproject.entity;

import com.example.diplomproject.enums.DiscountType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "promocodes")
@Data
@NoArgsConstructor
public class Promocode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String code;

    @Column(name = "discount_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private DiscountType discountType;

    @Column(name = "discount_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal value;

    @Column(name = "min_order_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal minOrderAmount;

    @Column(name = "valid_from", nullable = false)
    private LocalDateTime validFrom;

    @Column(name = "valid_to", nullable = false)
    private LocalDateTime validTo;

    @Column(name = "usage_limit", nullable = false)
    private Integer usageLimit;

    @Column(name = "used_count", nullable = false)
    private Integer usedCount=0;

    @ToString.Exclude
    @OneToMany(mappedBy = "promoCode",fetch = FetchType.LAZY)
    private List<Order> orders=new ArrayList<>();

    @ToString.Exclude
    @ManyToMany
    @JoinTable(name = "course_promos",
    joinColumns= @JoinColumn (name ="promocode_id"),
    inverseJoinColumns = @JoinColumn(name = "course_id")
    )
    private List<Course> applicableCourses=new ArrayList<>();

    @ToString.Exclude
    @ManyToMany
    @JoinTable(name = "category_promos",
    joinColumns = @JoinColumn(name = "promocode_id"),
    inverseJoinColumns=@JoinColumn( name="category_id")
    )
    private List<Category> applicableCategories=new ArrayList<>();

    @Column(nullable = false)
    private boolean active=true;

    @Column(name = "created_at", updatable = false,nullable=false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

}
