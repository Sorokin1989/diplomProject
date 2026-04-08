package com.example.diplomproject.repository;

import com.example.diplomproject.entity.CourseImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseImageRepository extends JpaRepository<CourseImage, Long> {

    List<CourseImage> findByCourseIdOrderBySortOrderAsc(Long courseId);

    void deleteByCourseId(Long courseId);
}