package com.example.diplomproject.service;

import com.example.diplomproject.dto.OrderDto;
import com.example.diplomproject.entity.*;
import com.example.diplomproject.enums.OrderStatus;
import com.example.diplomproject.mapper.OrderMapper;
import com.example.diplomproject.repository.OrderRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
public class OrderService {


    @PersistenceContext
    private EntityManager entityManager;

    private final OrderRepository orderRepository;
    private final OrderItemService orderItemService;
    private final CourseAccessService courseAccessService;
    private final CertificateService certificateService;
    private final CourseService courseService;
    private final UserService userService;
    private final OrderMapper orderMapper;
    private final PromocodeService promocodeService;

    @Autowired
    public OrderService(OrderRepository orderRepository,
                        OrderItemService orderItemService,
                        CourseAccessService courseAccessService,
                        CertificateService certificateService,
                        CourseService courseService,
                        UserService userService,
                        OrderMapper orderMapper, PromocodeService promocodeService) {
        this.orderRepository = orderRepository;
        this.orderItemService = orderItemService;
        this.courseAccessService = courseAccessService;
        this.certificateService = certificateService;
        this.courseService = courseService;
        this.userService = userService;
        this.orderMapper = orderMapper;
        this.promocodeService = promocodeService;
    }

    // ==================== Административные / внутренние методы (работа с сущностями) ====================

    @Transactional(readOnly = true)
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Order getOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Заказ не найден"));
    }

    @Transactional
    public Order createOrder(User user, List<OrderItem> orderItems) {



        if (user == null) {
            throw new IllegalArgumentException("Пользователь не может быть null");
        }
        if (orderItems == null || orderItems.isEmpty()) {
            throw new IllegalArgumentException("Заказ должен содержать хотя бы один элемент");
        }
        for (OrderItem item : orderItems) {
            if (item.getPrice() == null) throw new IllegalArgumentException("Цена элемента не может быть null");
            if (item.getCourse() == null) throw new IllegalArgumentException("Курс элемента не может быть null");
        }

        Order order = new Order();
        order.setUser(user);
        order.setOrderStatus(OrderStatus.PENDING);

        BigDecimal totalSum = orderItems.stream()
                .map(OrderItem::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setTotalSum(totalSum);

        Order savedOrder = orderRepository.save(order);

        for (OrderItem item : orderItems) {
            item.setOrder(savedOrder);
            orderItemService.createOrderItem(item);
        }

        return savedOrder;
    }

    @Transactional
    public Order updateOrderStatus(Long orderId, OrderStatus status) {
        Order order = getOrderById(orderId);
        order.setOrderStatus(status);
        Order saved = orderRepository.save(order);
        entityManager.flush();   // принудительно синхронизировать с БД
//        entityManager.clear();   // очистить кэш первого уровня
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Order> getOrdersByUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("Пользователь не может быть null");
        }
        return orderRepository.findByUser(user);
    }

    @Transactional
    public void deleteOrder(Long id) {
        Order order = getOrderById(id);
        orderRepository.delete(order);
    }

    @Transactional(readOnly = true)
    public boolean hasUserPurchasedCourse(User user, Course course) {
        return courseAccessService.hasAccessToUser(user, course);
    }

    @Transactional
    public Order updateOrder(Order order) {
        return orderRepository.save(order);
    }

    @Transactional
    public void hideAllOrdersByUser(User user) {
        List<Order> orders = orderRepository.findByUser(user);
        for (Order order : orders) {
            order.setHidden(true);
        }
        orderRepository.saveAll(orders);
    }

    @Transactional
    public void unhideAllOrdersByUser(User user) {
        List<Order> orders = orderRepository.findByUser(user);
        for (Order order : orders) {
            order.setHidden(false);
        }
        orderRepository.saveAll(orders);
    }

    // ==================== Пользовательские методы (работа с ID / DTO) ====================

    @Transactional(readOnly = true)
    public Order getOrderByIdWithItems(Long id) {
        return orderRepository.findByIdWithItems(id)
                .orElseThrow(() -> new NoSuchElementException("Заказ не найден"));
    }

    @Transactional
    public Order createOrderFromCourseIds(Long userId, List<Long> courseIds) {
        if (userId == null) {
            throw new IllegalArgumentException("ID пользователя не может быть null");
        }
        if (courseIds == null || courseIds.isEmpty()) {
            throw new IllegalArgumentException("Заказ должен содержать хотя бы один курс");
        }

        User user = userService.getUserById(userId);
        if (user == null) {
            throw new IllegalArgumentException("Пользователь не найден");
        }

        List<OrderItem> orderItems = new ArrayList<>();
        for (Long courseId : courseIds) {
            Course course = courseService.getCourseEntityById(courseId);
            if (course == null) {
                throw new IllegalArgumentException("Курс с id=" + courseId + " не найден");
            }
            OrderItem orderItem = new OrderItem();
            orderItem.setCourse(course);
            orderItem.setPrice(course.getPrice());
            orderItem.setCourseTitle(course.getTitle());
            orderItems.add(orderItem);
        }

        return createOrder(user, orderItems);
    }

    @Transactional(readOnly = true)
    public boolean hasUserPurchasedCourse(Long userId, Long courseId) {
        User user = userService.getUserById(userId);
        Course course = courseService.getCourseEntityById(courseId);
        return courseAccessService.hasAccessToUser(user, course);
    }

    @Transactional(readOnly = true)
    public List<Order> getOrdersByUserId(Long userId) {
        User user = userService.getUserById(userId);
        return getOrdersByUser(user);
    }

    // ==================== DTO-методы для пользовательской части ====================

    @Transactional(readOnly = true)
    public OrderDto getOrderDtoById(Long id) {
        Order order = getOrderByIdWithItems(id);  // ← исправлено
        return orderMapper.toOrderDTO(order);
    }

    @Transactional
    public OrderDto createOrderFromCourseIdsAndReturnDto(Long userId, List<Long> courseIds,
                                                         Promocode promocode, BigDecimal discountedTotal) {
        if (discountedTotal != null && discountedTotal.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Сумма со скидкой не может быть отрицательной");
        }
        Order order = createOrderFromCourseIds(userId, courseIds);
        BigDecimal originalTotal=order.getTotalSum();
        if (originalTotal != null) {
            order.setTotalSum(discountedTotal);
            BigDecimal discountAmount =originalTotal.subtract(discountedTotal);
            if(discountAmount.compareTo(BigDecimal.ZERO)>0){
                order.setDiscountAmount(discountAmount);
            }
        }
        if (promocode != null) {
            order.setPromoCode(promocode);
        }
        orderRepository.flush();
        entityManager.clear();
        Order orderWithItems = getOrderByIdWithItems(order.getId());
        return orderMapper.toOrderDTO(orderWithItems);
    }

    @Transactional(readOnly = true)
    public List<OrderDto> getOrderDtosByUser(User user) {
        return getOrdersByUser(user).stream()
                .map(orderMapper::toOrderDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrderDto> getAllOrderDtos() {
        return getAllOrders().stream()
                .map(orderMapper::toOrderDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteOrdersByUser(User user) {
        List<Order> orders = getOrdersByUser(user);
        orderRepository.deleteAll(orders);
    }
}