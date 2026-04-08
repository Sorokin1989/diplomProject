package com.example.diplomproject.mapper;

import com.example.diplomproject.dto.OrderItemDto;
import com.example.diplomproject.entity.OrderItem;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class OrderItemMapper {

    public OrderItemDto toOrderItemDto(OrderItem orderItem) {
        if (orderItem == null) return null;

        OrderItemDto dto = new OrderItemDto();
        dto.setId(orderItem.getId());
        dto.setQuantity(orderItem.getQuantity());
        dto.setPrice(orderItem.getPrice());

        // Вычисляем общую сумму (цена × количество)
        if (orderItem.getPrice() != null && orderItem.getQuantity() != null) {
            dto.setTotal(orderItem.getPrice().multiply(BigDecimal.valueOf(orderItem.getQuantity())));
        }

        if (orderItem.getCourse() != null) {
            dto.setCourseId(orderItem.getCourse().getId());
            dto.setCourseTitle(orderItem.getCourse().getTitle());
        } else {
            dto.setCourseTitle("Курс удален");
        }
        return dto;
    }

    public OrderItem fromOrderItemDtoToEntity(OrderItemDto dto) {
        if (dto == null) return null;

        OrderItem item = new OrderItem();
        item.setId(dto.getId());
        item.setQuantity(dto.getQuantity());
        item.setPrice(dto.getPrice());
        // Примечание: course и другие связи должны устанавливаться отдельно в сервисе
        return item;
    }
}