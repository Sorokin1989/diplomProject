package com.example.diplomproject.service;

import com.example.diplomproject.entity.OrderItem;
import com.example.diplomproject.repository.OrderItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class OrderItemService {

    @Autowired
    private OrderItemRepository orderItemRepository;

    /**
     * Получение всех элементов заказа
     */
    public List<OrderItem> getAllOrderItems() {
        return orderItemRepository.findAll();
    }

    /**
     * Получение элемента заказа по ID
     */
    public OrderItem getOrderItemById(Long id) {
        return orderItemRepository.findById(id).
                orElseThrow(()-> new NoSuchElementException("Такого заказа нет"));
    }

    /**
     * Создание нового элемента заказа
     */

    @Transactional
    public OrderItem createOrderItem(OrderItem orderItem) {
        if (orderItem == null) {
            throw new IllegalArgumentException("Элемент заказа не может быть null");
        }
        if (orderItem.getQuantity() == null || orderItem.getQuantity() <= 0) {
            throw new IllegalArgumentException("Количество должно быть больше нуля");
        }
        if (orderItem.getPrice() == null || orderItem.getPrice().compareTo(java.math.BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Цена не может быть отрицательной");
        }
        return orderItemRepository.save(orderItem);
    }

    /**
     * Обновление элемента заказа
     */
    @Transactional
    public OrderItem updateOrderItem(Long id, OrderItem updatedOrderItem) {
        OrderItem existingOrderItem = getOrderItemById(id);

        if (updatedOrderItem.getQuantity() != null && updatedOrderItem.getQuantity() > 0) {
            existingOrderItem.setQuantity(updatedOrderItem.getQuantity());
        }
        if (updatedOrderItem.getPrice() != null && updatedOrderItem.getPrice().compareTo(java.math.BigDecimal.ZERO) >= 0) {
            existingOrderItem.setPrice(updatedOrderItem.getPrice());
        }

        return orderItemRepository.save(existingOrderItem);
    }

    /**
     * Удаление элемента заказа
     */
    @Transactional
    public void deleteOrderItem(Long id) {
        OrderItem orderItem = getOrderItemById(id);
        orderItemRepository.delete(orderItem);
    }


}
