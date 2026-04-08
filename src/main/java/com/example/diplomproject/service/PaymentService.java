package com.example.diplomproject.service;

import com.example.diplomproject.entity.Order;
import com.example.diplomproject.entity.Payment;
import com.example.diplomproject.enums.PaymentStatus;
import com.example.diplomproject.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;

    @Autowired
    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    /**
     * Создание платежа для заказа
     */
    @Transactional
    public Payment createPayment(Order order, String paymentMethod) {
        if (order == null) {
            throw new IllegalArgumentException("Заказ не может быть null");
        }
        if (paymentMethod == null || paymentMethod.trim().isEmpty()) {
            throw new IllegalArgumentException("Метод оплаты должен указываться");
        }
        // Проверяем, нет ли уже платежа для этого заказа
        if (paymentRepository.findByOrder(order).isPresent()) {
            throw new IllegalStateException("Платёж для этого заказа уже существует");
        }

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setTotalSum(order.getTotalSum());
        payment.setPaymentMethod(paymentMethod);
        payment.setPaymentStatus(PaymentStatus.PENDING);
        payment.setCurrency("RUB");
        // createdAt и updatedAt установятся в @PrePersist
        return paymentRepository.save(payment);
    }

    /**
     * Подтверждение оплаты
     */
    @Transactional
    public Payment confirmPayment(Long paymentId) {
        Payment payment = getPaymentById(paymentId);
        if (payment.getPaymentStatus() != PaymentStatus.PENDING) {
            throw new IllegalStateException("Невозможно подтвердить платёж со статусом " + payment.getPaymentStatus());
        }
        payment.setPaymentStatus(PaymentStatus.SUCCESS);
        return paymentRepository.save(payment);
    }

    /**
     * Отмена платежа
     */
    @Transactional
    public Payment cancelPayment(Long paymentId) {
        Payment payment = getPaymentById(paymentId);
        if (payment.getPaymentStatus() != PaymentStatus.PENDING) {
            throw new IllegalStateException("Невозможно отменить платёж со статусом " + payment.getPaymentStatus());
        }
        payment.setPaymentStatus(PaymentStatus.CANCELLED);
        return paymentRepository.save(payment);
    }

    /**
     * Получение платежа по ID
     */
    public Payment getPaymentById(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Платёж с таким ID отсутствует"));
    }

    /**
     * Проверка статуса платежа
     */
    public boolean isPaymentSuccessful(Long paymentId) {
        Payment payment = getPaymentById(paymentId);
        return PaymentStatus.SUCCESS.equals(payment.getPaymentStatus());
    }

    /**
     * Поиск платежа по заказу
     */
    public Optional<Payment> getPaymentByOrder(Order order) {
        return paymentRepository.findByOrder(order);
    }
}