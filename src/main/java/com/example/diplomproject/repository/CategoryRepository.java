package com.example.diplomproject.repository;

import com.example.diplomproject.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    boolean existsByTitleContainingIgnoreCase(String title);

    Category getCategoryById(Long id);

    boolean existsByTitle(String title);

    boolean existsByTitleIgnoreCase(String title);
}
