package com.example.diplomproject.mapper;

import com.example.diplomproject.dto.OrderItemDto;
import com.example.diplomproject.entity.OrderItem;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Component
public class OrderItemMapper {
    private static final Logger log = LoggerFactory.getLogger(OrderItemMapper.class);

    public OrderItemDto toOrderItemDto(OrderItem orderItem) {
        if (orderItem == null) return null;

        OrderItemDto dto = new OrderItemDto();
        dto.setId(orderItem.getId());
        dto.setPrice(orderItem.getPrice());

        if (orderItem.getCourse() != null) {
            dto.setCourseId(orderItem.getCourse().getId());
            dto.setCourseTitle(orderItem.getCourse().getTitle());
        } else {
            dto.setCourseTitle("Курс удален");

        }
        return dto;
    }

//    public OrderItem fromOrderItemDtoToEntity(OrderItemDto dto) {
//        if (dto == null) return null;
//
//        OrderItem item = new OrderItem();
//        item.setId(dto.getId());
//        item.setPrice(dto.getPrice());
//        // Примечание: course и другие связи должны устанавливаться отдельно в сервисе
//        return item;
//    }
}