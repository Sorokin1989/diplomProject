package com.example.diplomproject.service;

import com.example.diplomproject.dto.CartDto;
import com.example.diplomproject.dto.CartItemDto;
import com.example.diplomproject.dto.CourseDto;
import com.example.diplomproject.entity.Cart;
import com.example.diplomproject.entity.CartItem;
import com.example.diplomproject.entity.Course;
import com.example.diplomproject.entity.User;
import com.example.diplomproject.mapper.CartItemMapper;
import com.example.diplomproject.mapper.CartMapper;
import com.example.diplomproject.repository.CartRepository;
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
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;
    @Mock
    private CartItemService cartItemService;
    @Mock
    private CartMapper cartMapper;
    @Mock
    private CartItemMapper cartItemMapper;
    @Mock
    private CourseService courseService;

    @InjectMocks
    private CartService cartService;

    private User user;
    private Cart cart;
    private Course course;
    private CartItem cartItem;
    private CartDto cartDto;
    private CartItemDto cartItemDto;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("testuser");

        cart = new Cart();
        cart.setId(10L);
        cart.setUser(user);

        course = new Course();
        course.setId(100L);
        course.setPrice(BigDecimal.valueOf(99.99));

        cartItem = new CartItem();
        cartItem.setId(200L);
        cartItem.setCart(cart);
        cartItem.setCourse(course);

        cartDto = new CartDto();
        cartDto.setId(10L);
        cartDto.setUserId(1L);

        cartItemDto = new CartItemDto();
        cartItemDto.setId(200L);
        cartItemDto.setPrice(BigDecimal.valueOf(99.99));
    }

    // ========== getOrCreateCart ==========
    @Test
    void getOrCreateCart_shouldReturnExistingCart() {
        when(cartRepository.findByUser(user)).thenReturn(Optional.of(cart));
        Cart result = cartService.getOrCreateCart(user);
        assertThat(result).isEqualTo(cart);
        verify(cartRepository, never()).save(any());
    }

    @Test
    void getOrCreateCart_shouldCreateNewCartWhenNotFound() {
        when(cartRepository.findByUser(user)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);
        Cart result = cartService.getOrCreateCart(user);
        assertThat(result).isEqualTo(cart);
        verify(cartRepository).save(any(Cart.class));
    }

    // ========== addCourseToCart (сущность) ==========
    @Test
    void addCourseToCart_shouldAddCourse() {
        when(cartRepository.findByUser(user)).thenReturn(Optional.of(cart));
        when(cartItemService.addCourseToCart(cart, course)).thenReturn(cartItem);
        CartItem result = cartService.addCourseToCart(user, course);
        assertThat(result).isEqualTo(cartItem);
        verify(cartItemService).addCourseToCart(cart, course);
    }

    // ========== addCourseToCart (CourseDto) ==========
    @Test
    void addCourseToCart_withCourseDto_shouldAddCourse() {
        CourseDto courseDto = new CourseDto();
        courseDto.setId(100L);
        when(courseService.getCourseEntityById(100L)).thenReturn(course);
        when(cartRepository.findByUser(user)).thenReturn(Optional.of(cart));
        when(cartItemService.addCourseToCart(cart, course)).thenReturn(cartItem);

        cartService.addCourseToCart(user, courseDto);

        verify(courseService).getCourseEntityById(100L);
        verify(cartItemService).addCourseToCart(cart, course);
    }

    // ========== removeCourseFromCart ==========
    @Test
    void removeCourseFromCart_shouldRemoveWhenBelongsToUser() {
        when(cartRepository.findByUser(user)).thenReturn(Optional.of(cart));
        when(cartItemService.getCartItemById(200L)).thenReturn(cartItem);
        cartService.removeCourseFromCart(user, 200L);
        verify(cartItemService).removeCartItem(200L);
    }

    @Test
    void removeCourseFromCart_shouldThrowWhenItemDoesNotBelongToUser() {
        Cart otherCart = new Cart();
        otherCart.setId(999L);
        CartItem otherItem = new CartItem();
        otherItem.setCart(otherCart);
        when(cartRepository.findByUser(user)).thenReturn(Optional.of(cart));
        when(cartItemService.getCartItemById(200L)).thenReturn(otherItem);

        assertThatThrownBy(() -> cartService.removeCourseFromCart(user, 200L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Этот элемент не принадлежит вашей корзине");
        verify(cartItemService, never()).removeCartItem(anyLong());
    }

    // ========== getAllItems ==========
    @Test
    void getAllItems_shouldReturnList() {
        when(cartRepository.findByUser(user)).thenReturn(Optional.of(cart));
        when(cartItemService.getCartItemsByCart(cart)).thenReturn(List.of(cartItem));
        List<CartItem> items = cartService.getAllItems(user);
        assertThat(items).hasSize(1);
        assertThat(items.get(0)).isEqualTo(cartItem);
    }

    @Test
    void getAllItems_shouldReturnEmptyListWhenNoCart() {
        when(cartRepository.findByUser(user)).thenReturn(Optional.empty());
        List<CartItem> items = cartService.getAllItems(user);
        assertThat(items).isEmpty();
        verify(cartItemService, never()).getCartItemsByCart(any());
    }

    // ========== clearCart ==========
    @Test
    void clearCart_shouldClear() {
        when(cartRepository.findByUser(user)).thenReturn(Optional.of(cart));
        cartService.clearCart(user);
        verify(cartItemService).clearCart(cart);
    }

    @Test
    void clearCart_shouldCreateCartIfNotExistsAndClear() {
        when(cartRepository.findByUser(user)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);
        cartService.clearCart(user);
        verify(cartItemService).clearCart(cart);
    }

    // ========== getTotalPrice ==========
    @Test
    void getTotalPrice_shouldSumPrices() {
        when(cartRepository.findByUser(user)).thenReturn(Optional.of(cart));
        when(cartItemService.getCartItemsByCart(cart)).thenReturn(List.of(cartItem));
        BigDecimal total = cartService.getTotalPrice(user);
        assertThat(total).isEqualByComparingTo("99.99");
    }

    // ========== isCourseInCart ==========
    @Test
    void isCourseInCart_shouldReturnTrueWhenExists() {
        when(cartRepository.findByUser(user)).thenReturn(Optional.of(cart));
        when(cartItemService.isCourseInCart(cart, course)).thenReturn(true);
        boolean result = cartService.isCourseInCart(user, course);
        assertThat(result).isTrue();
    }

    @Test
    void isCourseInCart_shouldReturnFalseWhenNoCart() {
        when(cartRepository.findByUser(user)).thenReturn(Optional.empty());
        boolean result = cartService.isCourseInCart(user, course);
        assertThat(result).isFalse();
        verify(cartItemService, never()).isCourseInCart(any(), any());
    }

    // ========== getCartDtoForUser ==========
    @Test
    void getCartDtoForUser_shouldReturnDto() {
        when(cartRepository.findByUser(user)).thenReturn(Optional.of(cart));
        when(cartMapper.toCartDTO(cart)).thenReturn(cartDto);
        CartDto result = cartService.getCartDtoForUser(user);
        assertThat(result).isEqualTo(cartDto);
    }

    @Test
    void getCartDtoForUser_shouldCreateCartIfNotExists() {
        when(cartRepository.findByUser(user)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);
        when(cartMapper.toCartDTO(cart)).thenReturn(cartDto);

        CartDto result = cartService.getOrCreateCartDto(user);  // ← заменили метод

        assertThat(result).isEqualTo(cartDto);
        verify(cartRepository).save(any(Cart.class));
    }

    // ========== getAllItemsDto ==========
    @Test
    void getAllItemsDto_shouldReturnMappedItems() {
        when(cartRepository.findByUser(user)).thenReturn(Optional.of(cart));
        when(cartItemMapper.toCartItemDTO(cartItem)).thenReturn(cartItemDto);
        cart.setCartItems(List.of(cartItem));
        List<CartItemDto> dtos = cartService.getAllItemsDto(user);
        assertThat(dtos).hasSize(1);
        assertThat(dtos.get(0)).isEqualTo(cartItemDto);
    }

    @Test
    void getAllItemsDto_shouldReturnEmptyListWhenNoCart() {
        when(cartRepository.findByUser(user)).thenReturn(Optional.empty());
        List<CartItemDto> dtos = cartService.getAllItemsDto(user);
        assertThat(dtos).isEmpty();
    }

    // ========== getTotalPriceDto ==========
    @Test
    void getTotalPriceDto_shouldSumPrices() {
        when(cartRepository.findByUser(user)).thenReturn(Optional.of(cart));
        when(cartItemMapper.toCartItemDTO(cartItem)).thenReturn(cartItemDto);
        cart.setCartItems(List.of(cartItem));
        BigDecimal total = cartService.getTotalPriceDto(user);
        assertThat(total).isEqualByComparingTo("99.99");
    }

    // ========== getCartItemCount ==========
    @Test
    void getCartItemCount_shouldReturnSize() {
        when(cartRepository.findByUser(user)).thenReturn(Optional.of(cart));
        cart.setCartItems(List.of(cartItem, cartItem));
        int count = cartService.getCartItemCount(user);
        assertThat(count).isEqualTo(2);
    }

    @Test
    void getCartItemCount_shouldReturnZeroWhenNoCart() {
        when(cartRepository.findByUser(user)).thenReturn(Optional.empty());
        int count = cartService.getCartItemCount(user);
        assertThat(count).isZero();
    }
}