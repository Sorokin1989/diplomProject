package com.example.diplomproject.service;

import com.example.diplomproject.entity.Order;
import com.example.diplomproject.entity.Payment;
import com.example.diplomproject.enums.PaymentStatus;
import com.example.diplomproject.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.NoSuchElementException;
import java.util.Optional;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentService paymentService;

    private Order order;
    private Payment payment;

    @BeforeEach
    void setUp() {
        order = new Order();
        order.setId(1L);
        order.setTotalSum(BigDecimal.valueOf(100));

        payment = new Payment();
        payment.setId(10L);
        payment.setOrder(order);
        payment.setPaymentStatus(PaymentStatus.PENDING);
        payment.setPaymentMethod("CARD");
    }

    @Test
    void createPayment_shouldSave() {
        when(paymentRepository.findByOrder(order)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        Payment result = paymentService.createPayment(order, "CARD");
        assertThat(result).isEqualTo(payment);
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void confirmPayment_shouldChangeStatus() {
        when(paymentRepository.findById(10L)).thenReturn(Optional.of(payment));
        Payment confirmed = paymentService.confirmPayment(10L);
        assertThat(confirmed.getPaymentStatus()).isEqualTo(PaymentStatus.SUCCESS);
        // Нет стаббинга save, так как он не вызывается в сервисе
        // При желании можно проверить, что save не вызывался:
        // verify(paymentRepository, never()).save(any());
    }

    @Test
    void cancelPayment_shouldChangeStatus() {
        when(paymentRepository.findById(10L)).thenReturn(Optional.of(payment));
        Payment cancelled = paymentService.cancelPayment(10L);
        assertThat(cancelled.getPaymentStatus()).isEqualTo(PaymentStatus.CANCELLED);
    }

    @Test
    void getPaymentById_shouldThrowWhenNotFound() {
        when(paymentRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> paymentService.getPaymentById(99L))
                .isInstanceOf(NoSuchElementException.class);
    }
}