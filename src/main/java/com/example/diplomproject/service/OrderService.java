package com.example.diplomproject.service;

import com.example.diplomproject.entity.Course;
import com.example.diplomproject.entity.Order;
import com.example.diplomproject.entity.OrderItem;
import com.example.diplomproject.entity.User;
import com.example.diplomproject.enums.OrderStatus;
import com.example.diplomproject.repository.CourseAccessRepository;
import com.example.diplomproject.repository.OrderItemRepository;
import com.example.diplomproject.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class OrderService {
    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;


    @Autowired
    private CourseAccessService courseAccessService;

    @Autowired
    private CertificateService certificateService;

    @Autowired
    private CourseAccessRepository courseAccessRepository;

    /**
     * Получение всех заказов
     */
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    /**
     * Получение заказа по ID
     */
    public Order getOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Заказ не найден"));
    }

    /**
     * Создание нового заказа
     */
    @Transactional
    public Order createOrder(User user, List<OrderItem> orderItems) {
        if (user == null) {
            throw new IllegalArgumentException("Пользователь не может быть null");
        }
        if (orderItems == null || orderItems.isEmpty()) {
            throw new IllegalArgumentException("Заказ должен содержать хотя бы один элемент");
        }

        Order order = new Order();
        order.setUser(user);
        order.setCreatedAt(LocalDateTime.now());
        order.setOrderStatus(OrderStatus.PENDING);

        BigDecimal totalSum = orderItems.stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        order.setTotalSum(totalSum);
        order.setOrderItems(orderItems);

        // Сохраняем заказ
        Order savedOrder = orderRepository.save(order);

        // Привязываем элементы к заказу и сохраняем их
        for (OrderItem item : orderItems) {
            item.setOrder(savedOrder);
            orderItemRepository.save(item);

            // Предоставляем доступ к курсу
            courseAccessService.grantAccessToCourse(user, item.getCourse());

            // Выдаём сертификат за покупку
            certificateService.generateCertificateForPurchase(user, item.getCourse());
        }

        return savedOrder;
    }

    /**
     * Обновление статуса заказа
     */
    @Transactional
    public Order updateOrderStatus(Long orderId,OrderStatus status){
        Order order=getOrderById(orderId);
        order.setOrderStatus(status);

        return orderRepository.save(order);

    }
    public List<Order> getOrdersByUser(User user){
        if (user==null){
            throw new IllegalArgumentException("Пользователь не может быть null");
        }
        return orderRepository.findByUser(user);
    }
    /**
     * Удаление заказа
     */

    public void deleteOrder(Long id){
        Order deleteOrder=getOrderById(id);
        orderRepository.delete(deleteOrder);
    }


    /**
     * Проверка на то что пользователь уже приобрел данный заказ
     */

    public boolean hasUserPurchasedCourse(User user, Course course) {
        return courseAccessRepository.existsByUserAndCourse(user, course);
    }
}
