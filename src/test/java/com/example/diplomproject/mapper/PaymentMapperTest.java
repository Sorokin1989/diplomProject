package com.example.diplomproject.mapper;

import com.example.diplomproject.dto.PaymentDto;
import com.example.diplomproject.entity.Order;
import com.example.diplomproject.entity.Payment;
import com.example.diplomproject.enums.OrderStatus;
import com.example.diplomproject.enums.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class PaymentMapperTest {

    private PaymentMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new PaymentMapper();
    }

    @Test
    void toPaymentDto_shouldMapFullPayment() {
        // given
        Order order = new Order();
        order.setId(10L);
        order.setOrderStatus(OrderStatus.PAID);
        order.setTotalSum(BigDecimal.valueOf(199.99));

        Payment payment = new Payment();
        payment.setId(1L);
        payment.setOrder(order);
        payment.setPaymentStatus(PaymentStatus.SUCCESS);
        payment.setPaymentMethod("CARD");
        payment.setTransactionId("txn_123");
        payment.setGatewayTransactionId("gtw_456");
        payment.setPaymentGateway("Stripe");
        payment.setCurrency("USD");
        payment.setFailureMessage(null);
        payment.setCreatedAt(LocalDateTime.of(2025, 1, 1, 12, 0));
        payment.setUpdatedAt(LocalDateTime.of(2025, 1, 1, 13, 0));

        // when
        PaymentDto dto = mapper.toPaymentDto(payment);

        // then
        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals(10L, dto.getOrderId());
        assertEquals("PAID", dto.getOrderStatus());
        assertEquals(0, BigDecimal.valueOf(199.99).compareTo(dto.getTotalSum()));
        assertEquals("SUCCESS", dto.getPaymentStatus());
        assertEquals("CARD", dto.getPaymentMethod());
        assertEquals("txn_123", dto.getTransactionId());
        assertEquals("gtw_456", dto.getGatewayTransactionId());
        assertEquals("Stripe", dto.getPaymentGateway());
        assertEquals("USD", dto.getCurrency());
        assertNull(dto.getFailureMessage());
        assertEquals(LocalDateTime.of(2025, 1, 1, 12, 0), dto.getCreatedAt());
        assertEquals(LocalDateTime.of(2025, 1, 1, 13, 0), dto.getUpdatedAt());
    }

    @Test
    void toPaymentDto_shouldHandleNullOrder() {
        // given
        Payment payment = new Payment();
        payment.setId(2L);
        payment.setOrder(null);
        payment.setPaymentStatus(PaymentStatus.PENDING);
        payment.setPaymentMethod("PAYPAL");

        // when
        PaymentDto dto = mapper.toPaymentDto(payment);

        // then
        assertNotNull(dto);
        assertNull(dto.getOrderId());
        assertNull(dto.getOrderStatus());
        assertNull(dto.getTotalSum());
        assertEquals("PENDING", dto.getPaymentStatus());
        assertEquals("PAYPAL", dto.getPaymentMethod());
    }

    @Test
    void toPaymentDto_shouldReturnNullForNullInput() {
        assertNull(mapper.toPaymentDto(null));
    }
}