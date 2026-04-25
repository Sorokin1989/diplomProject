package com.example.diplomproject.repository;

import com.example.diplomproject.entity.Course;
import com.example.diplomproject.entity.CourseAccess;
import com.example.diplomproject.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CourseAccessRepository extends JpaRepository<CourseAccess, Long> {

    boolean existsByUserAndCourse(User user, Course course);

    List<Course> findCoursesByUser(User user);

//    List<User> findUsersByCourse(Course course);
    @Query("SELECT ca.user FROM CourseAccess ca WHERE ca.course = :course")
    List<User> findUsersByCourse(@Param("course") Course course);


    Optional<CourseAccess> findByUserAndCourse(User user, Course course);

    // Новый метод с загрузкой изображений
    @Query("SELECT ca.course FROM CourseAccess ca JOIN FETCH ca.course.images WHERE ca.user = :user")
    List<Course> findCoursesByUserWithImages(@Param("user") User user);

    boolean existsByUserAndCourseId(User user, Long id);
    boolean existsByCourseId(Long courseId);
}
