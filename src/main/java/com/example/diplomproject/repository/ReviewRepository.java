package com.example.diplomproject.repository;

import com.example.diplomproject.entity.Course;
import com.example.diplomproject.entity.Review;
import com.example.diplomproject.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    Optional<Review> findByUserAndCourse(User user, Course course);

    List<Review> findByCourse(Course course);

    boolean existsByUserAndCourse(User user, Course course);
}
