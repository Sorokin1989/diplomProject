package com.example.diplomproject.repository;

import com.example.diplomproject.dto.CertificateDto;
import com.example.diplomproject.entity.Certificate;
import com.example.diplomproject.entity.Course;
import com.example.diplomproject.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CertificateRepository extends JpaRepository<Certificate, Long> {

    boolean existsByUserAndCourse(User user, Course course);

    List<Certificate> findByUser(User user);

    List<Certificate> findByCourse(Course course);

    @Query("SELECT c FROM Certificate c JOIN FETCH c.user JOIN FETCH c.course")
    List<Certificate> findAllWithUserAndCourse();

    List<Certificate> findByCourseAndRevokedFalse(Course course);

    void deleteByCourseAndRevokedTrue(Course course);

    @Query("SELECT c FROM Certificate c JOIN FETCH c.user JOIN FETCH c.course WHERE c.user = :user")
    List<Certificate> findByUserWithDetails(@Param("user") User user);

    @Query("SELECT c FROM Certificate c JOIN FETCH c.user JOIN FETCH c.course WHERE c.id = :id")
    Optional<Certificate> findByIdWithDetails(@Param("id") Long id);

    Optional<Certificate> findByUserIdAndCourseId(Long userId, Long courseId);
}