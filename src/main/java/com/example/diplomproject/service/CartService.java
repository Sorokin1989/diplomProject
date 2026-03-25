package com.example.diplomproject.service;

import com.example.diplomproject.entity.Cart;
import com.example.diplomproject.entity.CartItem;
import com.example.diplomproject.entity.Course;
import com.example.diplomproject.entity.User;
import com.example.diplomproject.repository.CartRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemService cartItemService;

    @Autowired
    public CartService(CartRepository cartRepository, CartItemService cartItemService) {
        this.cartRepository = cartRepository;
        this.cartItemService = cartItemService;
    }

    /**
     * Получение корзины пользователя (создаёт, если нет)
     */
    @Transactional
    public Cart getOrCreateCart(User user) {
        return cartRepository.findByUser(user).orElseGet(() -> {
            Cart newCart = new Cart();
            newCart.setUser(user);
            return cartRepository.save(newCart);
        });
    }

    /**
     * Добавление курса в корзину
     */
    @Transactional
    public CartItem addCourseToCart(User user, Course course, int quantity) {
        Cart cart = getOrCreateCart(user);
        return cartItemService.addCourseToCart(cart, course, quantity);
    }

    /**
     * Удаление курса из корзины
     */
    @Transactional
    public void removeCourseFromCart(User user, Long cartItemId) {
        Cart cart = getOrCreateCart(user);

        CartItem cartItem = cartItemService.getCartItemById(cartItemId);

        if (!cartItem.getCart().equals(cart)) {
            throw new IllegalArgumentException("Этот элемент не принадлежит вашей корзине");
        }
        cartItemService.removeCartItem(cartItemId);


    }

    /**
     * Обновление количества курсов в корзине
     */
    @Transactional
    public CartItem updateQuantity(User user, Long cartItemId, int quantity) {
        Cart cart = getOrCreateCart(user);
        CartItem cartItem = cartItemService.getCartItemById(cartItemId);
        if (!cartItem.getCart().equals(cart)) {
            throw new IllegalArgumentException("Этот элемент не пренадлжит вашей корзине");
        }
        return cartItemService.updateQuantity(cartItemId, quantity);
    }

    /**
     * Получение всех элементов корзины пользователя
     */
    public List<CartItem> getAllItems(User user) {
        Cart cart = getOrCreateCart(user);
        return cartItemService.getCartItemsByCart(cart);
    }

    /**
     * Очистка корзины (после оформления заказа)
     */

    @Transactional
    public void clearCart(User user) {
        Cart cart = getOrCreateCart(user);
        cartItemService.clearCart(cart);
    }

    /**
     * Подсчёт общей стоимости корзины
     */

    public BigDecimal getTotalPrice(User user) {
        return getAllItems(user).stream()
                .map(item -> item.getCourse().getPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Проверка, находится ли курс в корзине
     */

    public boolean isCourseInCart(User user, Course course) {
        Cart cart = getOrCreateCart(user);
        return cartItemService.isCourseInCart(cart, course);
    }


}
