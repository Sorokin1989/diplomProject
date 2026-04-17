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

    private final OrderItemRepository orderItemRepository;

    @Autowired
    public OrderItemService(OrderItemRepository orderItemRepository) {
        this.orderItemRepository = orderItemRepository;
    }

    /**
     * Получение всех элементов заказа
     */
    @Transactional(readOnly = true)
    public List<OrderItem> getAllOrderItems() {
        return orderItemRepository.findAll();
    }

    /**
     * Получение элемента заказа по ID
     */
    @Transactional(readOnly = true)
    public OrderItem getOrderItemById(Long id) {
        return orderItemRepository.findById(id).
                orElseThrow(()-> new NoSuchElementException("Элемент заказа не найден с id: " + id));
    }

    /**
     * Создание нового элемента заказа
     */

    @Transactional
    public OrderItem createOrderItem(OrderItem orderItem) {
        if (orderItem == null) {
            throw new IllegalArgumentException("Элемент заказа не может быть null");
        }

        if (orderItem.getOrder() == null || orderItem.getCourse() == null) {
            throw new IllegalArgumentException("Order и Course должны быть установлены");
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
        if (updatedOrderItem == null) {
            throw new IllegalArgumentException("Обновляемые данные не могут быть null");
        }
        OrderItem existingOrderItem = getOrderItemById(id);

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
