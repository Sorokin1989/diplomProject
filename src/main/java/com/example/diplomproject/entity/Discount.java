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
@Table(name = "discounts")
@Data
@NoArgsConstructor
public class Discount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false)
    private String title;

    @Column(columnDefinition="TEXT")
    private String description;

    @Column(nullable= false,name = "discount_type")
    @Enumerated(EnumType.STRING)
    private DiscountType discountType;

    @Column(name = "discount_value",precision =10,scale = 2,nullable = false)
    private BigDecimal discountValue;

    @Column(name = "start_date",nullable = false)
    private LocalDateTime startDate;

    @Column (name ="end_date",nullable = false)
    private LocalDateTime endDate;

    @ToString.Exclude
    @ManyToMany
    @JoinTable(name = "courses_discounts",
            joinColumns = @JoinColumn( name = "discount_id"),
            inverseJoinColumns = @JoinColumn(name ="course_id")
    )
    private List<Course> applicableCourses=new ArrayList<>();

    @ToString.Exclude
    @ManyToMany
    @JoinTable(
            name = "categories_discounts",
            joinColumns=@JoinColumn(name = "discount_id"),
            inverseJoinColumns =@JoinColumn(name = "category_id")
    )
    private List<Category>applicableCategories=new ArrayList<>();

    @Column(name = "min_order_amount")
    private BigDecimal minOrderAmount;

    @Column(nullable = false)
    private boolean active=true;

    @Column(name = "created_at", nullable =false,updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate(){
        createdAt=LocalDateTime.now();
    }

}
