package com.example.diplomproject.service;

import com.example.diplomproject.dto.OrderDto;
import com.example.diplomproject.entity.*;
import com.example.diplomproject.enums.OrderStatus;
import com.example.diplomproject.mapper.OrderMapper;
import com.example.diplomproject.repository.OrderRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderItemService orderItemService;
    @Mock
    private CourseAccessService courseAccessService;
    @Mock
    private CertificateService certificateService;
    @Mock
    private CourseService courseService;
    @Mock
    private UserService userService;
    @Mock
    private OrderMapper orderMapper;
    @Mock
    private PromocodeService promocodeService;
    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private OrderService orderService;

    private User user;
    private Course course;
    private OrderItem orderItem;
    private Order order;
    private Promocode promocode;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("testuser");

        course = new Course();
        course.setId(10L);
        course.setTitle("Test Course");
        course.setPrice(BigDecimal.valueOf(99.99));

        orderItem = new OrderItem();
        orderItem.setId(100L);
        orderItem.setCourse(course);
        orderItem.setPrice(course.getPrice());
        orderItem.setCourseTitle(course.getTitle());

        order = new Order();
        order.setId(1000L);
        order.setUser(user);
        order.setOrderStatus(OrderStatus.PENDING);
        order.setTotalSum(BigDecimal.valueOf(99.99));
        order.setOrderItems(List.of(orderItem));

        promocode = new Promocode();
        promocode.setId(5L);
        promocode.setCode("SAVE10");
        ReflectionTestUtils.setField(orderService, "entityManager", entityManager);
    }

    // ========== getAllOrders ==========
    @Test
    void getAllOrders_shouldReturnList() {
        when(orderRepository.findAll()).thenReturn(List.of(order));
        List<Order> result = orderService.getAllOrders();
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(order);
    }

    // ========== getOrderById ==========
    @Test
    void getOrderById_shouldReturnOrder() {
        when(orderRepository.findById(1000L)).thenReturn(Optional.of(order));
        Order found = orderService.getOrderById(1000L);
        assertThat(found).isEqualTo(order);
    }

    @Test
    void getOrderById_shouldThrowWhenNotFound() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> orderService.getOrderById(999L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("Заказ не найден");
    }

    // ========== createOrder ==========
    @Test
    void createOrder_shouldSaveValidOrder() {
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(orderItemService.createOrderItem(any(OrderItem.class))).thenReturn(orderItem);

        Order saved = orderService.createOrder(user, List.of(orderItem));

        assertThat(saved).isEqualTo(order);
        verify(orderRepository).save(any(Order.class));
        verify(orderItemService).createOrderItem(any(OrderItem.class));
    }

    @Test
    void createOrder_shouldThrowWhenUserNull() {
        assertThatThrownBy(() -> orderService.createOrder(null, List.of(orderItem)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Пользователь не может быть null");
        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrder_shouldThrowWhenOrderItemsEmpty() {
        assertThatThrownBy(() -> orderService.createOrder(user, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Заказ должен содержать хотя бы один элемент");
        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrder_shouldThrowWhenOrderItemPriceNull() {
        orderItem.setPrice(null);
        assertThatThrownBy(() -> orderService.createOrder(user, List.of(orderItem)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Цена элемента не может быть null");
        verify(orderRepository, never()).save(any());
    }

    // ========== updateOrderStatus ==========
    @Test
    void updateOrderStatus_shouldChangeStatus() {
        when(orderRepository.findById(1000L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        Order updated = orderService.updateOrderStatus(1000L, OrderStatus.PAID);

        assertThat(updated.getOrderStatus()).isEqualTo(OrderStatus.PAID);
        verify(orderRepository).save(order);
    }

    // ========== getOrdersByUser ==========
    @Test
    void getOrdersByUser_shouldReturnList() {
        when(orderRepository.findByUser(user)).thenReturn(List.of(order));
        List<Order> result = orderService.getOrdersByUser(user);
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(order);
    }

    @Test
    void getOrdersByUser_shouldThrowWhenUserNull() {
        assertThatThrownBy(() -> orderService.getOrdersByUser(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ========== deleteOrder ==========
    @Test
    void deleteOrder_shouldDelete() {
        when(orderRepository.findById(1000L)).thenReturn(Optional.of(order));
        orderService.deleteOrder(1000L);
        verify(orderRepository).delete(order);
    }

    // ========== hasUserPurchasedCourse (сущности) ==========
    @Test
    void hasUserPurchasedCourse_shouldReturnTrue() {
        when(courseAccessService.hasAccessToUser(user, course)).thenReturn(true);
        boolean result = orderService.hasUserPurchasedCourse(user, course);
        assertThat(result).isTrue();
    }

    // ========== createOrderFromCourseIds ==========
    @Test
    void createOrderFromCourseIds_shouldCreateOrder() {
        when(userService.getUserById(1L)).thenReturn(user);
        when(courseService.getCourseEntityById(10L)).thenReturn(course);
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(orderItemService.createOrderItem(any(OrderItem.class))).thenReturn(orderItem);

        Order result = orderService.createOrderFromCourseIds(1L, List.of(10L));

        assertThat(result).isEqualTo(order);
        verify(orderItemService).createOrderItem(any(OrderItem.class));
    }

    @Test
    void createOrderFromCourseIds_shouldThrowWhenCourseNotFound() {
        when(userService.getUserById(1L)).thenReturn(user);
        when(courseService.getCourseEntityById(99L)).thenThrow(new IllegalArgumentException("Курс не найден"));
        assertThatThrownBy(() -> orderService.createOrderFromCourseIds(1L, List.of(99L)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ========== hasUserPurchasedCourse (Long) ==========
    @Test
    void hasUserPurchasedCourse_withIds_shouldReturnTrue() {
        when(userService.getUserById(1L)).thenReturn(user);
        when(courseService.getCourseEntityById(10L)).thenReturn(course);
        when(courseAccessService.hasAccessToUser(user, course)).thenReturn(true);
        boolean result = orderService.hasUserPurchasedCourse(1L, 10L);
        assertThat(result).isTrue();
    }

    // ========== getOrderDtoById ==========
    @Test
    void getOrderDtoById_shouldReturnDto() {
        when(orderRepository.findByIdWithItems(1000L)).thenReturn(Optional.of(order));
        when(orderMapper.toOrderDTO(order)).thenReturn(new OrderDto());
        OrderDto dto = orderService.getOrderDtoById(1000L);
        assertThat(dto).isNotNull();
    }

    // ========== createOrderFromCourseIdsAndReturnDto ==========
    @Test
    void createOrderFromCourseIdsAndReturnDto_shouldReturnDto() {
        when(userService.getUserById(1L)).thenReturn(user);
        when(courseService.getCourseEntityById(10L)).thenReturn(course);
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(orderItemService.createOrderItem(any(OrderItem.class))).thenReturn(orderItem);
        when(orderRepository.findByIdWithItems(1000L)).thenReturn(Optional.of(order));
        when(orderMapper.toOrderDTO(order)).thenReturn(new OrderDto());

        OrderDto dto = orderService.createOrderFromCourseIdsAndReturnDto(1L, List.of(10L), promocode, BigDecimal.valueOf(89.99));

        assertThat(dto).isNotNull();
        verify(orderRepository, atLeastOnce()).save(any(Order.class));
    }

    @Test
    void createOrderFromCourseIdsAndReturnDto_shouldThrowWhenNegativeDiscount() {
        assertThatThrownBy(() -> orderService.createOrderFromCourseIdsAndReturnDto(1L, List.of(10L), null, BigDecimal.valueOf(-10)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Сумма со скидкой не может быть отрицательной");
    }

    // ========== getOrderDtosByUser ==========
    @Test
    void getOrderDtosByUser_shouldReturnList() {
        when(orderRepository.findByUser(user)).thenReturn(List.of(order));
        when(orderMapper.toOrderDTO(order)).thenReturn(new OrderDto());
        List<OrderDto> dtos = orderService.getOrderDtosByUser(user);
        assertThat(dtos).hasSize(1);
    }

    // ========== deleteOrdersByUser ==========
    @Test
    void deleteOrdersByUser_shouldDeleteAll() {
        when(orderRepository.findByUser(user)).thenReturn(List.of(order));
        orderService.deleteOrdersByUser(user);
        verify(orderRepository).deleteAll(List.of(order));
    }
}