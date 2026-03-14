package com.example.diplomproject.mapper;

import com.example.diplomproject.dto.CartDto;
import com.example.diplomproject.dto.CartItemDto;
import com.example.diplomproject.entity.Cart;
import com.example.diplomproject.entity.CartItem;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CartMapper {

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
                    stream().map(this::toCartItemDto).toList();

            cartDto.setCartItems(cartItemDtos);
        }


        if (cart.getUser() != null) {
            cartDto.setUserId(cart.getUser().getId());
            cartDto.setUsername(cart.getUser().getName());
        }

        return cartDto;


    }

    private CartItemDto toCartItemDto(CartItem cartItem) {

        CartItemDto cartItemDto = new CartItemDto();
        cartItemDto.setId(cartItem.getId());
        cartItemDto.setCourseId(cartItem.getCourse().getId());
        cartItemDto.setCourseTitle(cartItem.getCourse().getTitle());
        cartItemDto.setPrice(cartItem.getPrice());
        cartItemDto.setAddedAt(cartItem.getAddedAt().toString());
        return cartItemDto;

    }
}
