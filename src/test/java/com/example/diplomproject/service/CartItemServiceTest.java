package com.example.diplomproject.service;

import com.example.diplomproject.entity.Cart;
import com.example.diplomproject.entity.CartItem;
import com.example.diplomproject.entity.Course;
import com.example.diplomproject.repository.CartItemRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartItemServiceTest {

    @Mock
    private CartItemRepository cartItemRepository;

    @InjectMocks
    private CartItemService cartItemService;

    private Cart cart;
    private Course course;
    private CartItem cartItem;

    @BeforeEach
    void setUp() {
        cart = new Cart();
        cart.setId(1L);

        course = new Course();
        course.setId(10L);
        course.setPrice(BigDecimal.valueOf(99.99));

        cartItem = new CartItem();
        cartItem.setId(100L);
        cartItem.setCart(cart);
        cartItem.setCourse(course);
        cartItem.setPrice(course.getPrice());
    }

    // ========== addCourseToCart ==========

    @Test
    void addCourseToCart_shouldAddNewCourse() {
        when(cartItemRepository.findByCartAndCourse(cart, course)).thenReturn(null);
        when(cartItemRepository.save(any(CartItem.class))).thenReturn(cartItem);

        CartItem result = cartItemService.addCourseToCart(cart, course);

        assertThat(result).isNotNull();
        assertThat(result.getCart()).isEqualTo(cart);
        assertThat(result.getCourse()).isEqualTo(course);
        verify(cartItemRepository).save(any(CartItem.class));
    }

    @Test
    void addCourseToCart_shouldThrowWhenCartIsNull() {
        assertThatThrownBy(() -> cartItemService.addCourseToCart(null, course))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cart и Course не могут быть null");
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    void addCourseToCart_shouldThrowWhenCourseIsNull() {
        assertThatThrownBy(() -> cartItemService.addCourseToCart(cart, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cart и Course не могут быть null");
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    void addCourseToCart_shouldThrowWhenCoursePriceIsNull() {
        course.setPrice(null);
        assertThatThrownBy(() -> cartItemService.addCourseToCart(cart, course))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Цена курса не может быть null");
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    void addCourseToCart_shouldThrowWhenCourseAlreadyInCart() {
        when(cartItemRepository.findByCartAndCourse(cart, course)).thenReturn(cartItem);

        assertThatThrownBy(() -> cartItemService.addCourseToCart(cart, course))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Курс уже добавлен в корзину");
        verify(cartItemRepository, never()).save(any());
    }

    // ========== removeCartItem ==========

    @Test
    void removeCartItem_shouldDeleteWhenExists() {
        when(cartItemRepository.findById(100L)).thenReturn(Optional.of(cartItem));

        cartItemService.removeCartItem(100L);

        verify(cartItemRepository).delete(cartItem);
    }

    @Test
    void removeCartItem_shouldThrowWhenNotExists() {
        when(cartItemRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartItemService.removeCartItem(999L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Элемент корзины не найден с id: 999");
        verify(cartItemRepository, never()).delete(any());
    }

    // ========== getCartItemsByCart ==========

    @Test
    void getCartItemsByCart_shouldReturnList() {
        when(cartItemRepository.findByCart(cart)).thenReturn(List.of(cartItem));

        List<CartItem> items = cartItemService.getCartItemsByCart(cart);

        assertThat(items).hasSize(1);
        assertThat(items.get(0)).isEqualTo(cartItem);
        verify(cartItemRepository).findByCart(cart);
    }

    @Test
    void getCartItemsByCart_shouldThrowWhenCartIsNull() {
        assertThatThrownBy(() -> cartItemService.getCartItemsByCart(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cart не может быть null");
        verify(cartItemRepository, never()).findByCart(any());
    }

    // ========== clearCart ==========

    @Test
    void clearCart_shouldDeleteAllItems() {
        cartItemService.clearCart(cart);
        verify(cartItemRepository).deleteByCart(cart);
    }

    @Test
    void clearCart_shouldThrowWhenCartIsNull() {
        assertThatThrownBy(() -> cartItemService.clearCart(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cart не может быть null");
        verify(cartItemRepository, never()).deleteByCart(any());
    }

    // ========== getCartItemById ==========

    @Test
    void getCartItemById_shouldReturnItemWhenExists() {
        when(cartItemRepository.findById(100L)).thenReturn(Optional.of(cartItem));

        CartItem found = cartItemService.getCartItemById(100L);

        assertThat(found).isEqualTo(cartItem);
        verify(cartItemRepository).findById(100L);
    }

    @Test
    void getCartItemById_shouldThrowWhenNotExists() {
        when(cartItemRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartItemService.getCartItemById(999L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Элемент корзины не найден с id: 999");
    }

    // ========== isCourseInCart ==========

    @Test
    void isCourseInCart_shouldReturnTrueWhenCourseExists() {
        when(cartItemRepository.findByCartAndCourse(cart, course)).thenReturn(cartItem);

        boolean result = cartItemService.isCourseInCart(cart, course);

        assertThat(result).isTrue();
    }

    @Test
    void isCourseInCart_shouldReturnFalseWhenCourseNotExists() {
        when(cartItemRepository.findByCartAndCourse(cart, course)).thenReturn(null);

        boolean result = cartItemService.isCourseInCart(cart, course);

        assertThat(result).isFalse();
    }

    @Test
    void isCourseInCart_shouldThrowWhenCartIsNull() {
        assertThatThrownBy(() -> cartItemService.isCourseInCart(null, course))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cart и Course не могут быть null");
        verify(cartItemRepository, never()).findByCartAndCourse(any(), any());
    }

    @Test
    void isCourseInCart_shouldThrowWhenCourseIsNull() {
        assertThatThrownBy(() -> cartItemService.isCourseInCart(cart, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cart и Course не могут быть null");
        verify(cartItemRepository, never()).findByCartAndCourse(any(), any());
    }
}