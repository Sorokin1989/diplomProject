package com.example.diplomproject.repository;

import com.example.diplomproject.entity.Course;
import com.example.diplomproject.entity.CourseAccess;
import com.example.diplomproject.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CourseAccessRepository extends JpaRepository<CourseAccess, Long> {

    boolean existsByUserAndCourse(User user, Course course);

    List<Course> findCoursesByUser(User user);

    List<User> findUsersByCourse(Course course);


    Optional<CourseAccess> findByUserAndCourse(User user, Course course);
}
