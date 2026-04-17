package com.example.diplomproject.service;

import com.example.diplomproject.entity.Course;
import com.example.diplomproject.entity.Order;
import com.example.diplomproject.entity.OrderItem;
import com.example.diplomproject.repository.OrderItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderItemServiceTest {

    @Mock
    private OrderItemRepository orderItemRepository;

    @InjectMocks
    private OrderItemService orderItemService;

    private OrderItem orderItem;
    private Order order;
    private Course course;

    @BeforeEach
    void setUp() {
        order = new Order();
        order.setId(10L);

        course = new Course();
        course.setId(100L);
        course.setTitle("Test Course");

        orderItem = new OrderItem();
        orderItem.setId(1L);
        orderItem.setOrder(order);
        orderItem.setCourse(course);
        orderItem.setPrice(BigDecimal.valueOf(99.99));
        orderItem.setCourseTitle("Test Course");
    }

    // ========== getAllOrderItems ==========
    @Test
    void getAllOrderItems_shouldReturnList() {
        when(orderItemRepository.findAll()).thenReturn(List.of(orderItem));
        List<OrderItem> result = orderItemService.getAllOrderItems();
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(orderItem);
        verify(orderItemRepository).findAll();
    }

    // ========== getOrderItemById ==========
    @Test
    void getOrderItemById_shouldReturnOrderItem() {
        when(orderItemRepository.findById(1L)).thenReturn(Optional.of(orderItem));
        OrderItem result = orderItemService.getOrderItemById(1L);
        assertThat(result).isEqualTo(orderItem);
    }

    @Test
    void getOrderItemById_shouldThrowWhenNotFound() {
        when(orderItemRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> orderItemService.getOrderItemById(99L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("Элемент заказа не найден с id: 99");
    }

    // ========== createOrderItem ==========
    @Test
    void createOrderItem_shouldSaveValidOrderItem() {
        when(orderItemRepository.save(any(OrderItem.class))).thenReturn(orderItem);
        OrderItem saved = orderItemService.createOrderItem(orderItem);
        assertThat(saved).isEqualTo(orderItem);
        verify(orderItemRepository).save(orderItem);
    }

    @Test
    void createOrderItem_shouldThrowWhenOrderItemIsNull() {
        assertThatThrownBy(() -> orderItemService.createOrderItem(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Элемент заказа не может быть null");
        verify(orderItemRepository, never()).save(any());
    }

    @Test
    void createOrderItem_shouldThrowWhenOrderIsNull() {
        orderItem.setOrder(null);
        assertThatThrownBy(() -> orderItemService.createOrderItem(orderItem))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Order и Course должны быть установлены");
        verify(orderItemRepository, never()).save(any());
    }

    @Test
    void createOrderItem_shouldThrowWhenCourseIsNull() {
        orderItem.setCourse(null);
        assertThatThrownBy(() -> orderItemService.createOrderItem(orderItem))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Order и Course должны быть установлены");
        verify(orderItemRepository, never()).save(any());
    }

    @Test
    void createOrderItem_shouldThrowWhenPriceIsNull() {
        orderItem.setPrice(null);
        assertThatThrownBy(() -> orderItemService.createOrderItem(orderItem))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Цена не может быть отрицательной");
        verify(orderItemRepository, never()).save(any());
    }

    @Test
    void createOrderItem_shouldThrowWhenPriceIsNegative() {
        orderItem.setPrice(BigDecimal.valueOf(-10));
        assertThatThrownBy(() -> orderItemService.createOrderItem(orderItem))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Цена не может быть отрицательной");
        verify(orderItemRepository, never()).save(any());
    }

    // ========== updateOrderItem ==========
    @Test
    void updateOrderItem_shouldUpdatePrice() {
        OrderItem updatedData = new OrderItem();
        updatedData.setPrice(BigDecimal.valueOf(49.99));

        when(orderItemRepository.findById(1L)).thenReturn(Optional.of(orderItem));
        when(orderItemRepository.save(any(OrderItem.class))).thenReturn(orderItem);

        OrderItem result = orderItemService.updateOrderItem(1L, updatedData);

        assertThat(result.getPrice()).isEqualByComparingTo("49.99");
        verify(orderItemRepository).save(orderItem);
    }

    @Test
    void updateOrderItem_shouldThrowWhenUpdatedDataIsNull() {
        assertThatThrownBy(() -> orderItemService.updateOrderItem(1L, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Обновляемые данные не могут быть null");
        verify(orderItemRepository, never()).save(any());
    }

    @Test
    void updateOrderItem_shouldIgnoreNullPrice() {
        OrderItem updatedData = new OrderItem();
        updatedData.setPrice(null);

        when(orderItemRepository.findById(1L)).thenReturn(Optional.of(orderItem));
        when(orderItemRepository.save(any(OrderItem.class))).thenReturn(orderItem);

        OrderItem result = orderItemService.updateOrderItem(1L, updatedData);

        assertThat(result.getPrice()).isEqualByComparingTo("99.99"); // цена не изменилась
        verify(orderItemRepository).save(orderItem);
    }

    // ========== deleteOrderItem ==========
    @Test
    void deleteOrderItem_shouldDelete() {
        when(orderItemRepository.findById(1L)).thenReturn(Optional.of(orderItem));
        orderItemService.deleteOrderItem(1L);
        verify(orderItemRepository).delete(orderItem);
    }

    @Test
    void deleteOrderItem_shouldThrowWhenNotFound() {
        when(orderItemRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> orderItemService.deleteOrderItem(99L))
                .isInstanceOf(NoSuchElementException.class);
        verify(orderItemRepository, never()).delete(any());
    }
}