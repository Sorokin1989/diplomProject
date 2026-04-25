package com.example.diplomproject.service;

import com.example.diplomproject.entity.Cart;
import com.example.diplomproject.entity.CartItem;
import com.example.diplomproject.entity.Course;
import com.example.diplomproject.repository.CartItemRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CartItemService {

    private final CartItemRepository cartItemRepository;

    @Autowired
    public CartItemService(CartItemRepository cartItemRepository) {
        this.cartItemRepository = cartItemRepository;
    }

    /**
     * Добавление курса в корзину (только один экземпляр курса)
     */
    @Transactional
    public CartItem addCourseToCart(Cart cart, Course course) {
        if (cart == null || course == null) {
            throw new IllegalArgumentException("Cart и Course не могут быть null");
        }
        if (course.getPrice() == null) {
            throw new IllegalArgumentException("Цена курса не может быть null");
        }

        // Проверяем, есть ли уже такой курс в корзине
        CartItem existingItem = cartItemRepository.findByCartAndCourse(cart, course);
        if (existingItem != null) {
            throw new IllegalStateException("Курс уже добавлен в корзину");
        }

        CartItem newItem = new CartItem();
        newItem.setCart(cart);
        newItem.setCourse(course);
        newItem.setPrice(course.getPrice());

        return cartItemRepository.save(newItem);
    }

    /**
     * Удаление элемента из корзины
     */
    @Transactional
    public void removeCartItem(Long cartItemId) {
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new EntityNotFoundException("Элемент корзины не найден с id: " + cartItemId));
        cartItemRepository.delete(item);
    }

    /**
     * Получение всех элементов корзины
     */
    @Transactional(readOnly = true)
    public List<CartItem> getCartItemsByCart(Cart cart) {
        if (cart == null) {
            throw new IllegalArgumentException("Cart не может быть null");
        }
        return cartItemRepository.findByCart(cart);
    }

    /**
     * Очистка корзины
     */
    @Transactional
    public void clearCart(Cart cart) {
        if (cart == null) {
            throw new IllegalArgumentException("Cart не может быть null");
        }
        cartItemRepository.deleteByCart(cart);
    }

    /**
     * Получение элемента корзины по ID
     */
    @Transactional(readOnly = true)
    public CartItem getCartItemById(Long cartItemId) {
        return cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new EntityNotFoundException("Элемент корзины не найден с id: " + cartItemId));
    }

    /**
     * Проверка, находится ли курс в корзине
     */
    @Transactional(readOnly = true)
    public boolean isCourseInCart(Cart cart, Course course) {
        if (cart == null || course == null) {
            throw new IllegalArgumentException("Cart и Course не могут быть null");
        }
        return cartItemRepository.findByCartAndCourse(cart, course) != null;
    }
    @Transactional(readOnly = true)
    public boolean existsByCourseId(Long id) {
        if (id == null) return false;
        return cartItemRepository.existsByCourseId(id);
    }
}