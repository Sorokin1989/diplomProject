package com.example.diplomproject.repository;

import com.example.diplomproject.entity.CategoryImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryImageRepository extends JpaRepository<CategoryImage, Long> {
    void deleteByCategoryId(Long categoryId);

    List<CategoryImage> findByCategoryIdOrderBySortOrderAsc(Long id);
}