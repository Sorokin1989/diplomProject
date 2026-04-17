package com.example.diplomproject.mapper;

import com.example.diplomproject.dto.OrderDto;
import com.example.diplomproject.entity.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OrderMapper {

    private final CourseAccessMapper courseAccessMapper;
    private final OrderItemMapper orderItemMapper;

    @Autowired
    public OrderMapper(CourseAccessMapper courseAccessMapper, OrderItemMapper orderItemMapper) {
        this.courseAccessMapper = courseAccessMapper;
        this.orderItemMapper = orderItemMapper;
    }

    public OrderDto toOrderDTO(Order order) {

        if (order == null) {
            return null;
        }

        OrderDto orderDto = new OrderDto();
        orderDto.setId(order.getId());

        if (order.getUser() != null) {
            orderDto.setUserId(order.getUser().getId());
            orderDto.setUsername(order.getUser().getUsername());
        }
        orderDto.setCreatedAt(order.getCreatedAt());
        orderDto.setUpdatedAt(order.getUpdatedAt());
        orderDto.setTotalSum(order.getTotalSum());
        orderDto.setOrderStatus(String.valueOf(order.getOrderStatus()));

        if (order.getOrderItems() != null) {
            orderDto.setOrderItemDtos(order.getOrderItems().stream().map(orderItem -> this.orderItemMapper.toOrderItemDto(orderItem)).toList());

        }

        if (order.getCourseAccesses() != null) {
            orderDto.setCourseAccessDtos(order.getCourseAccesses().stream().map(courseAccess -> this.courseAccessMapper.toCourseAccessDto(courseAccess)).toList());
        }

        if(order.getPayment()!=null){
        orderDto.setPaymentId(order.getPayment().getId());
        orderDto.setPaymentType(order.getPayment().getPaymentMethod());   // добавить
            orderDto.setPaymentStatus(String.valueOf(order.getPayment().getPaymentStatus()));
        }

        orderDto.setDiscountAmount(order.getDiscountAmount());

        if(order.getPromoCode()!=null){
        orderDto.setPromoCode(order.getPromoCode().getCode());
        }
        return orderDto;
    }
//    public Order fromOrderDtoToEntity(OrderDto orderDto){
//        if (orderDto==null){
//            return null;
//        }
//        Order order=new Order();
//        order.setId(orderDto.getId());
//        order.setCreatedAt(orderDto.getCreatedAt());
//        order.setUpdatedAt(orderDto.getUpdatedAt());
//        order.setTotalSum(orderDto.getTotalSum());
//        order.setOrderStatus(OrderStatus.valueOf(orderDto.getOrderStatus()));
//
//return order;
//    }
}
