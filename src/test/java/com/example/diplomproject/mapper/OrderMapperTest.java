package com.example.diplomproject.mapper;

import com.example.diplomproject.dto.OrderDto;
import com.example.diplomproject.entity.*;
import com.example.diplomproject.enums.OrderStatus;
import com.example.diplomproject.enums.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderMapperTest {

    @Mock
    private CourseAccessMapper courseAccessMapper;

    @Mock
    private OrderItemMapper orderItemMapper;

    @InjectMocks
    private OrderMapper orderMapper;

    private Order order;
    private User user;
    private Payment payment;
    private Promocode promoCode;
    private OrderItem orderItem;
    private CourseAccess courseAccess;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("testuser");

        payment = new Payment();
        payment.setId(100L);
        payment.setPaymentMethod("CARD");
        payment.setPaymentStatus(PaymentStatus.SUCCESS);

        promoCode = new Promocode();
        promoCode.setCode("SAVE10");

        orderItem = new OrderItem();
        orderItem.setId(10L);

        courseAccess = new CourseAccess();
        courseAccess.setId(20L);

        order = new Order();
        order.setId(1L);
        order.setUser(user);
        order.setCreatedAt(LocalDateTime.of(2025, 1, 1, 12, 0));
        order.setUpdatedAt(LocalDateTime.of(2025, 1, 1, 13, 0));
        order.setTotalSum(BigDecimal.valueOf(199.99));
        order.setOrderStatus(OrderStatus.PAID);
        order.setOrderItems(List.of(orderItem));
        order.setCourseAccesses(List.of(courseAccess));
        order.setPayment(payment);
        order.setDiscountAmount(BigDecimal.valueOf(20));
        order.setPromoCode(promoCode);
    }

    @Test
    void toOrderDTO_shouldMapFullOrder() {
        // given
        when(orderItemMapper.toOrderItemDto(orderItem)).thenReturn(null); // можно замокать реальный DTO
        when(courseAccessMapper.toCourseAccessDto(courseAccess)).thenReturn(null);

        // when
        OrderDto dto = orderMapper.toOrderDTO(order);

        // then
        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getUserId()).isEqualTo(1L);
        assertThat(dto.getUsername()).isEqualTo("testuser");
        assertThat(dto.getCreatedAt()).isEqualTo(LocalDateTime.of(2025, 1, 1, 12, 0));
        assertThat(dto.getUpdatedAt()).isEqualTo(LocalDateTime.of(2025, 1, 1, 13, 0));
        assertThat(dto.getTotalSum()).isEqualByComparingTo("199.99");
        assertThat(dto.getOrderStatus()).isEqualTo("PAID");
        assertThat(dto.getOrderItemDtos()).hasSize(1);
        assertThat(dto.getCourseAccessDtos()).hasSize(1);
        assertThat(dto.getPaymentId()).isEqualTo(100L);
        assertThat(dto.getPaymentType()).isEqualTo("CARD");
        assertThat(dto.getPaymentStatus()).isEqualTo("SUCCESS");
        assertThat(dto.getDiscountAmount()).isEqualByComparingTo("20");
        assertThat(dto.getPromoCode()).isEqualTo("SAVE10");
    }

    @Test
    void toOrderDTO_shouldHandleNullRelations() {
        // given
        order.setUser(null);
        order.setOrderItems(null);
        order.setCourseAccesses(null);
        order.setPayment(null);
        order.setPromoCode(null);

        // when
        OrderDto dto = orderMapper.toOrderDTO(order);

        // then
        assertThat(dto).isNotNull();
        assertThat(dto.getUserId()).isNull();
        assertThat(dto.getUsername()).isNull();
        assertThat(dto.getOrderItemDtos()).isNull();
        assertThat(dto.getCourseAccessDtos()).isNull();
        assertThat(dto.getPaymentId()).isNull();
        assertThat(dto.getPaymentType()).isNull();
        assertThat(dto.getPaymentStatus()).isNull();
        assertThat(dto.getPromoCode()).isNull();
    }

    @Test
    void toOrderDTO_shouldReturnNullForNullInput() {
        assertThat(orderMapper.toOrderDTO(null)).isNull();
    }
}