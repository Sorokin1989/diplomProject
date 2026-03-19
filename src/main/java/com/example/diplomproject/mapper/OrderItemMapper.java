package com.example.diplomproject.mapper;

import com.example.diplomproject.dto.OrderItemDto;
import com.example.diplomproject.entity.OrderItem;
import org.springframework.stereotype.Component;

@Component
public class OrderItemMapper {

    public OrderItemDto toOrderItemDto(OrderItem orderItem) {

        if (orderItem == null){
            return null;
        }

        OrderItemDto orderItemDto = new OrderItemDto();

        orderItemDto.setId(orderItem.getId());

        if (orderItem.getCourse() !=null){
        orderItemDto.setCourseId(orderItem.getCourse().getId());
        orderItemDto.setCourseTitle(orderItem.getCourse().getTitle());
        }
        else {
            orderItemDto.setCourseTitle("Курс удален");
        }
        orderItemDto.setPrice(orderItem.getPrice());

        return orderItemDto;
    }

    public OrderItem fromOrderItemDtoToEntity(OrderItemDto orderItemDto){

        if(orderItemDto==null){
            return null;
        }

        OrderItem orderItem=new OrderItem();
        orderItem.setId(orderItemDto.getId());
        orderItem.setPrice(orderItemDto.getPrice());

        return orderItem;
    }

}
