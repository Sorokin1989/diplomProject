package com.example.diplomproject.entity;


import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@Data
@Table(name = "courses")
@NoArgsConstructor
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false,unique=true)
    private String title;

    @Column(columnDefinition="TEXT")
    private String description;

    @Column(precision=10, scale=2)
    private BigDecimal price;

    @Column(nullable=false)
    private String author;

    @Column
    private boolean isActive=true;

    @ManyToOne(fetch=LAZY)
    @JoinColumn(name = "category_id")
    private Category category;


}
