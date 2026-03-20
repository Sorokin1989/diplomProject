package com.example.diplomproject.service;

import com.example.diplomproject.entity.Cart;
import com.example.diplomproject.entity.CartItem;
import com.example.diplomproject.entity.Course;
import com.example.diplomproject.repository.CartItemRepository;
import com.example.diplomproject.repository.CartRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CartItemService {

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private CartRepository cartRepository;

    /**
     * Добавление курса в корзину
     */
    @Transactional
    public CartItem addCourseToCart(Cart cart, Course course, int quantity) {
        // Проверяем, есть ли уже такой курс в корзине
        CartItem existingItem = cartItemRepository.findByCartAndCourse(cart, course);
        if (existingItem != null) {
            // Если есть — увеличиваем количество
            existingItem.setQuantity(existingItem.getQuantity() + quantity);
            return cartItemRepository.save(existingItem);
        } else {
            // Если нет — создаём новый элемент
            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setCourse(course);
            newItem.setQuantity(quantity);
            return cartItemRepository.save(newItem);
        }
    }

    /**
     * Удаление элемента из корзины
     */
    @Transactional
    public void removeCartItem(Long cartItemId) {
        cartItemRepository.deleteById(cartItemId);
    }

    /**
     * Обновление количества курсов в элементе корзины
     */
    @Transactional
    public CartItem updateQuantity(Long cartItemId, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Количество должно быть больше 0");
        }

        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new IllegalArgumentException("Элемент корзины не найден"));

        item.setQuantity(quantity);
        return cartItemRepository.save(item);
    }

    /**
     * Получение всех элементов корзины
     */
    public List<CartItem> getCartItemsByCart(Cart cart) {
        return cartItemRepository.findByCart(cart);
    }

    /**
     * Очистка корзины
     */
    @Transactional
    public void clearCart(Cart cart) {
        cartItemRepository.deleteByCart(cart);
    }

    /**
     * Получение элемента корзины по ID
     */
    public CartItem getCartItemById(Long cartItemId) {
        return cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new IllegalArgumentException("Элемент корзины не найден"));
    }

    /**
     * Проверка, находится ли курс в корзине
     */
    public boolean isCourseInCart(Cart cart, Course course) {
        return cartItemRepository.findByCartAndCourse(cart, course) != null;
    }
}