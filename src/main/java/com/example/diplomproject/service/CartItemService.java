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
     * Добавление курса в корзину
     */
    @Transactional
    public CartItem addCourseToCart(Cart cart, Course course, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Количество должно быть больше 0");
        }

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
            newItem.setPrice(course.getPrice());
            newItem.setQuantity(quantity);
            return cartItemRepository.save(newItem);
        }
    }

    /**
     * Удаление элемента из корзины
     */
    @Transactional
    public void removeCartItem(Long cartItemId) {
        if (!cartItemRepository.existsById(cartItemId)) {
            throw new EntityNotFoundException("Элемент корзины не найден с id: " + cartItemId);
        }
        cartItemRepository.deleteById(cartItemId);
    }

    /**
     * Обновление количества курсов в элементе корзины.
     * Если quantity <= 0, элемент удаляется.
     * @return CartItem если количество > 0, иначе null (элемент удалён)
     */
    @Transactional
    public CartItem updateQuantity(Long cartItemId, int quantity) {
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new EntityNotFoundException("Элемент корзины не найден с id: " + cartItemId));

        if (quantity <= 0) {
            // Удаляем элемент, если количество равно нулю или отрицательное
            cartItemRepository.delete(item);
            return null;
        }

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
                .orElseThrow(() -> new EntityNotFoundException("Элемент корзины не найден с id: " + cartItemId));
    }

    /**
     * Проверка, находится ли курс в корзине
     */
    public boolean isCourseInCart(Cart cart, Course course) {
        return cartItemRepository.findByCartAndCourse(cart, course) != null;
    }
}