package com.example.diplomproject.controller;

import com.example.diplomproject.entity.Order;
import com.example.diplomproject.entity.Payment;
import com.example.diplomproject.entity.User;
import com.example.diplomproject.enums.OrderStatus;
import com.example.diplomproject.service.OrderService;
import com.example.diplomproject.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Controller
@RequestMapping("/payment")
public class PaymentController {

    private final PaymentService paymentService;
    private final OrderService orderService;

    @Autowired
    public PaymentController(PaymentService paymentService, OrderService orderService) {
        this.paymentService = paymentService;
        this.orderService = orderService;
    }

    /**
     * Страница оплаты для заказа (доступна владельцу заказа или админу)
     */
    @GetMapping("/order/{orderId}")
    public String showPaymentPage(@PathVariable Long orderId,
                                  @AuthenticationPrincipal User currentUser,
                                  Model model) {
        Order order = orderService.getOrderById(orderId);
        // Проверка прав: только владелец заказа или администратор
        if (!order.getUser().getId().equals(currentUser.getId()) && !isAdmin(currentUser)) {
            return "error/403";
        }

        // Если уже есть платеж для этого заказа, берём его, иначе создаём
        Optional<Payment> existingPayment = paymentService.getPaymentByOrder(order);
        Payment payment;
        if (existingPayment.isPresent()) {
            payment = existingPayment.get();
        } else {
            // Метод оплаты можно передать из формы, здесь просто значение по умолчанию
            payment = paymentService.createPayment(order, "CARD");
        }

        model.addAttribute("payment", payment);
        model.addAttribute("order", order);
        return "pages/payment/page";
    }

    /**
     * Обработка подтверждения оплаты (например, после успешного списания)
     */
    @PostMapping("/confirm/{paymentId}")
    public String confirmPayment(@PathVariable Long paymentId,
                                 @AuthenticationPrincipal User currentUser) {
        Payment payment = paymentService.getPaymentById(paymentId);
        Order order = payment.getOrder();
        // Проверка прав: только владелец заказа или админ
        if (!order.getUser().getId().equals(currentUser.getId()) && !isAdmin(currentUser)) {
            return "error/403";
        }

        paymentService.confirmPayment(paymentId);
        // После подтверждения обновляем статус заказа на PAID
        orderService.updateOrderStatus(order.getId(), OrderStatus.PAID);
        return "redirect:/orders/" + order.getId();
    }

    /**
     * Отмена оплаты (пользователь передумал)
     */
    @PostMapping("/cancel/{paymentId}")
    public String cancelPayment(@PathVariable Long paymentId,
                                @AuthenticationPrincipal User currentUser) {
        Payment payment = paymentService.getPaymentById(paymentId);
        Order order = payment.getOrder();
        if (!order.getUser().getId().equals(currentUser.getId()) && !isAdmin(currentUser)) {
            return "error/403";
        }

        paymentService.cancelPayment(paymentId);
        return "redirect:/orders/" + order.getId();
    }

    private boolean isAdmin(User user) {
        return user != null && user.getRole() != null && "ADMIN".equalsIgnoreCase(user.getRole().name());
    }
}