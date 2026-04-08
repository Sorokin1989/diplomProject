package com.example.diplomproject.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "category_images")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String filePath;   // относительный путь, например /uploads/categories/123.jpg

    @Column(nullable = false)
    private boolean main;      // главное изображение (используется для превью)

    private int sortOrder;     // порядок отображения

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;
}