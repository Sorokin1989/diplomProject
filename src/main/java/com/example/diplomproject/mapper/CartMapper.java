package com.example.diplomproject.mapper;

import com.example.diplomproject.dto.CartDto;
import com.example.diplomproject.dto.CartItemDto;
import com.example.diplomproject.entity.Cart;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CartMapper {

    @Autowired
    private CartItemMapper cartItemMapper;

    public CartDto toCartDTO(Cart cart) {

        if (cart == null) {
            return null;
        }
        CartDto cartDto = new CartDto();
        cartDto.setId(cart.getId());
        cartDto.setSessionId(cart.getSessionId());
        cartDto.setCreatedAt(cart.getCreatedAt());
        cartDto.setUpdatedAt(cart.getUpdatedAt());

        if (cart.getCartItems() != null) {
            List<CartItemDto> cartItemDtos = cart.getCartItems().
                    stream().map(cartItem -> cartItemMapper.toCartItemDTO(cartItem)).toList();

            cartDto.setCartItems(cartItemDtos);
        }


        if (cart.getUser() != null) {
            cartDto.setUserId(cart.getUser().getId());
            cartDto.setUsername(cart.getUser().getUsername());
        }

        return cartDto;
    }

    public Cart fromCartDtoToEntity(CartDto cartDto) {
        if (cartDto == null) {
            return null;
        }
        Cart cart = new Cart();

        cart.setId(cartDto.getId());
        cart.setSessionId(cartDto.getSessionId());
        cart.setCreatedAt(cartDto.getCreatedAt());
        cart.setUpdatedAt(cartDto.getUpdatedAt());

        if (cartDto.getCartItems() != null) {
            cart.setCartItems(cartDto.getCartItems().stream().
                    map(cartItemDto -> cartItemMapper.fromCartItemDtoToEntity(cartItemDto)).toList());
        }

        return cart;

    }
}
