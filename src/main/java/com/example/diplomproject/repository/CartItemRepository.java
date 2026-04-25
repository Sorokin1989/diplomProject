package com.example.diplomproject.repository;

import com.example.diplomproject.entity.Cart;
import com.example.diplomproject.entity.CartItem;
import com.example.diplomproject.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    CartItem findByCartAndCourse(Cart cart, Course course);

    List<CartItem> findByCart(Cart cart);

    void deleteByCart(Cart cart);

    boolean existsByCourseId(Long courseId);
}
