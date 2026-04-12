package com.example.diplomproject.repository;

import com.example.diplomproject.entity.Discount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DiscountRepository extends JpaRepository<Discount, Long> {

    @Query("SELECT d FROM Discount d WHERE d.active = true AND d.startDate <= :now AND d.endDate >= :now")
    List<Discount> findActiveAtDate(@Param("now") LocalDateTime now);
}
