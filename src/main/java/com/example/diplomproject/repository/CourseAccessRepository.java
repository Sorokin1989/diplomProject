package com.example.diplomproject.repository;

import com.example.diplomproject.entity.CourseAccess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseAccessRepository extends JpaRepository<CourseAccess, Long> {

}
