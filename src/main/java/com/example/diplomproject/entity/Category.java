package com.example.diplomproject.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "categories")
@Data
@NoArgsConstructor
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ToString.Exclude
    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
    private List<Course> courses = new ArrayList<>();

    @ToString.Exclude
    @ManyToMany(mappedBy = "applicableCategories", fetch = FetchType.LAZY)
    private List<Promocode> promocodes = new ArrayList<>();

    @ToString.Exclude
    @ManyToMany(mappedBy = "applicableCategories",fetch=FetchType.LAZY)
    private List<Discount> discounts = new ArrayList<>();

    @ToString.Exclude
    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<CategoryImage> images = new ArrayList<>();

    // Вспомогательный метод для получения главного изображения
    public String getMainImageUrl() {
        return images.stream()
                .filter(CategoryImage::isMain)
                .findFirst()
                .map(CategoryImage::getFilePath)
                .orElse(null);
    }
}
