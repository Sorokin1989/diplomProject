package com.example.diplomproject.repository;

import com.example.diplomproject.entity.Category;
import com.example.diplomproject.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {

    List<Course> findByCategory(Category category);

    List<Course> findByTitleContainingIgnoreCase(String trim);
}
