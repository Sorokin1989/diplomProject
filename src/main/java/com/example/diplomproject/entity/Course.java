package com.example.diplomproject.entity;


import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
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


    @ToString.Exclude
    @OneToMany(mappedBy = "course", fetch = FetchType.LAZY)
    private List<Review> reviews=new ArrayList<>();

    @ToString.Exclude
    @OneToMany(mappedBy="course",fetch=LAZY)
    private List<Bonus> bonuses=new ArrayList<>();

    @ToString.Exclude
    @OneToMany(mappedBy = "course",fetch = LAZY)
    private List<Certificate>certificates=new ArrayList<>();

}
