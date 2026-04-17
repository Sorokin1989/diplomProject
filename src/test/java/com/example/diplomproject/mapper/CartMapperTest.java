package com.example.diplomproject.mapper;

import com.example.diplomproject.dto.CartDto;
import com.example.diplomproject.dto.CartItemDto;
import com.example.diplomproject.entity.Cart;
import com.example.diplomproject.entity.CartItem;
import com.example.diplomproject.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartMapperTest {

    @Mock
    private CartItemMapper cartItemMapper;

    @InjectMocks
    private CartMapper cartMapper;

    private Cart cart;
    private User user;
    private CartItem cartItem;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("testuser");

        cartItem = new CartItem();
        cartItem.setId(100L);

        cart = new Cart();
        cart.setId(10L);
        cart.setSessionId("session123");
        cart.setUser(user);
        cart.setCartItems(List.of(cartItem));
    }

    @Test
    void toCartDTO_shouldMapFullCart() {
        when(cartItemMapper.toCartItemDTO(cartItem)).thenReturn(new CartItemDto());

        CartDto dto = cartMapper.toCartDTO(cart);

        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(10L);
        assertThat(dto.getSessionId()).isEqualTo("session123");
        assertThat(dto.getUserId()).isEqualTo(1L);
        assertThat(dto.getUsername()).isEqualTo("testuser");
        assertThat(dto.getCartItems()).hasSize(1);
    }

    @Test
    void toCartDTO_shouldReturnNullForNullInput() {
        assertThat(cartMapper.toCartDTO(null)).isNull();
    }

    @Test
    void toCartDTO_shouldHandleCartWithoutUser() {
        cart.setUser(null);
        CartDto dto = cartMapper.toCartDTO(cart);
        assertThat(dto.getUserId()).isNull();
        assertThat(dto.getUsername()).isNull();
    }

    @Test
    void toCartDTO_shouldHandleNullCartItems() {
        cart.setCartItems(null);
        CartDto dto = cartMapper.toCartDTO(cart);
        assertThat(dto.getCartItems()).isNull();
    }
}