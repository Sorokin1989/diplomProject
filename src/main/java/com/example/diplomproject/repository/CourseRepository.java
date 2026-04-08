package com.example.diplomproject.repository;

import com.example.diplomproject.entity.Category;
import com.example.diplomproject.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {

    List<Course> findByCategory(Category category);

    List<Course> findByTitleContainingIgnoreCase(String trim);
    List<Course> findByCategoryId(Long categoryId);

    @Query("SELECT c FROM Course c LEFT JOIN FETCH c.images WHERE c.id = :id")
    Optional<Course> findByIdWithImages(@Param("id") Long id);

    @Query("SELECT c FROM Course c LEFT JOIN FETCH c.images WHERE c.category.id = :categoryId")
    List<Course> findByCategoryIdWithImages(@Param("categoryId") Long categoryId);

    @Query("SELECT c FROM Course c LEFT JOIN FETCH c.images WHERE LOWER(c.title) LIKE LOWER(CONCAT('%', :title, '%'))")
    List<Course> findByTitleContainingIgnoreCaseWithImages(@Param("title") String title);

    @Query("SELECT c FROM Course c LEFT JOIN FETCH c.images")
    List<Course> findAllWithImages();
}

