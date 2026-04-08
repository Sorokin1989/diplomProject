package com.example.diplomproject.service;

import com.example.diplomproject.dto.CartDto;
import com.example.diplomproject.dto.CartItemDto;
import com.example.diplomproject.entity.Cart;
import com.example.diplomproject.entity.CartItem;
import com.example.diplomproject.entity.Course;
import com.example.diplomproject.entity.User;
import com.example.diplomproject.mapper.CartItemMapper;
import com.example.diplomproject.mapper.CartMapper;
import com.example.diplomproject.repository.CartRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemService cartItemService;
    private final CartMapper cartMapper;
    private final CartItemMapper cartItemMapper;

    @Autowired
    public CartService(CartRepository cartRepository, CartItemService cartItemService,
                       CartMapper cartMapper, CartItemMapper cartItemMapper) {
        this.cartRepository = cartRepository;
        this.cartItemService = cartItemService;
        this.cartMapper = cartMapper;
        this.cartItemMapper = cartItemMapper;
    }

    // ==================== Методы для работы с сущностями (для админки) ====================

    @Transactional
    public Cart getOrCreateCart(User user) {
        return cartRepository.findByUser(user).orElseGet(() -> {
            Cart newCart = new Cart();
            newCart.setUser(user);
            return cartRepository.save(newCart);
        });
    }

    @Transactional
    public CartItem addCourseToCart(User user, Course course, int quantity) {
        Cart cart = getOrCreateCart(user);
        return cartItemService.addCourseToCart(cart, course, quantity);
    }

    @Transactional
    public void removeCourseFromCart(User user, Long cartItemId) {
        Cart cart = getOrCreateCart(user);
        CartItem cartItem = cartItemService.getCartItemById(cartItemId);
        if (!cartItem.getCart().equals(cart)) {
            throw new IllegalArgumentException("Этот элемент не принадлежит вашей корзине");
        }
        cartItemService.removeCartItem(cartItemId);
    }

    @Transactional
    public CartItem updateQuantity(User user, Long cartItemId, int quantity) {
        Cart cart = getOrCreateCart(user);
        CartItem cartItem = cartItemService.getCartItemById(cartItemId);
        if (!cartItem.getCart().equals(cart)) {
            throw new IllegalArgumentException("Этот элемент не принадлежит вашей корзине");
        }
        return cartItemService.updateQuantity(cartItemId, quantity);
    }

    public List<CartItem> getAllItems(User user) {
        return cartRepository.findByUser(user)
                .map(cart -> cartItemService.getCartItemsByCart(cart))
                .orElse(Collections.emptyList());
    }

    @Transactional
    public void clearCart(User user) {
        Cart cart = getOrCreateCart(user);
        cartItemService.clearCart(cart);
    }

    public BigDecimal getTotalPrice(User user) {
        return getAllItems(user).stream()
                .filter(item -> item.getCourse() != null && item.getCourse().getPrice() != null)
                .map(item -> item.getCourse().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public boolean isCourseInCart(User user, Course course) {
        return cartRepository.findByUser(user)
                .map(cart -> cartItemService.isCourseInCart(cart, course))
                .orElse(false);
    }

    public CartDto getCartDtoForUser(User user) {
        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Корзина не найдена"));
        return cartMapper.toCartDTO(cart);
    }

    // ==================== DTO-методы для пользовательской части ====================

    @Transactional(readOnly = true)
    public CartDto getOrCreateCartDto(User user) {
        Cart cart = cartRepository.findByUser(user)
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setUser(user);
                    return cartRepository.save(newCart);
                });
        return cartMapper.toCartDTO(cart);
    }

    @Transactional(readOnly = true)
    public List<CartItemDto> getAllItemsDto(User user) {
        return cartRepository.findByUser(user)
                .map(cart -> cart.getCartItems().stream()
                        .map(cartItemMapper::toCartItemDTO)
                        .collect(Collectors.toList()))
                .orElse(Collections.emptyList());
    }

    // Метод переименован, чтобы не конфликтовать с сущностной версией getTotalPrice
    @Transactional(readOnly = true)
    public BigDecimal getTotalPriceDto(User user) {
        return getAllItemsDto(user).stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}