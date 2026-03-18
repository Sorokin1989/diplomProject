package com.example.diplomproject.entity;


import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@Data
@Table(name = "courses")
@NoArgsConstructor
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private String author;

    @Column
    private boolean isActive = true;

    @Column(nullable=false)
    private String imageUrl;

    @Column(nullable= false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt=LocalDateTime.now();
    }

    @Column(nullable = false)
    private Integer reviewCount;


    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "category_id")
    private Category category;


    @ToString.Exclude
    @OneToMany(mappedBy = "course", fetch = FetchType.LAZY)
    private List<Review> reviews = new ArrayList<>();

    @ToString.Exclude
    @OneToMany(mappedBy = "course", fetch = LAZY)
    private List<Bonus> bonuses = new ArrayList<>();

    @ToString.Exclude
    @OneToMany(mappedBy = "course", fetch = LAZY)
    private List<Certificate> certificates = new ArrayList<>();

    @ToString.Exclude
    @OneToMany(mappedBy = "course", fetch = LAZY)
    private List<OrderItem> orderItems = new ArrayList();


    @ToString.Exclude
    @OneToMany(mappedBy = "course", fetch = LAZY)
    private List<CartItem> cartItems = new ArrayList();

    @ToString.Exclude
    @ManyToMany(mappedBy = "applicableCourses",fetch=LAZY)
    private List<Promocode> promoCodes=new ArrayList();

    @ToString.Exclude
    @ManyToMany(mappedBy = "applicableCourses")
    private List<Discount>discounts=new ArrayList();

    @ToString.Exclude
    @OneToMany(mappedBy="course",fetch = LAZY)
    private List<CourseAccess>courseAccesses=new ArrayList();


}
