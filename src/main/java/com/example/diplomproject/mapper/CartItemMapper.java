package com.example.diplomproject.mapper;

import com.example.diplomproject.dto.CartItemDto;
import com.example.diplomproject.entity.CartItem;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
public class CartItemMapper {
    public CartItemDto toCartItemDTO(CartItem cartItem) {
        if (cartItem == null) {
            return null;
        }
        CartItemDto cartItemDto = new CartItemDto();
        cartItemDto.setId(cartItem.getId());

        if (cartItem.getCourse() != null) {
            cartItemDto.setCourseId(cartItem.getCourse().getId());
            cartItemDto.setCourseTitle(cartItem.getCourse().getTitle());
        } else {
            cartItemDto.setCourseTitle("Курс удален");
        }
        cartItemDto.setPrice(cartItem.getPrice() != null ? cartItem.getPrice() : BigDecimal.ZERO);
        cartItemDto.setAddedAt(cartItem.getAddedAt() != null ? cartItem.getAddedAt() : LocalDateTime.now());
        cartItemDto.setQuantity(cartItem.getQuantity());

        return cartItemDto;
    }

    public CartItem fromCartItemDtoToEntity(CartItemDto cartItemDto){

        if (cartItemDto==null){
            return null;
        }
        CartItem cartItem=new CartItem();
        cartItem.setId(cartItemDto.getId());
        cartItem.setPrice(cartItemDto.getPrice());
        cartItem.setAddedAt(cartItemDto.getAddedAt());
        cartItem.setQuantity(cartItemDto.getQuantity());

        return cartItem;


    }

}
