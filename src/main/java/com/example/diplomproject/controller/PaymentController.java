package com.example.diplomproject.controller;

import com.example.diplomproject.dto.PaymentDto;
import com.example.diplomproject.entity.Order;
import com.example.diplomproject.entity.Payment;
import com.example.diplomproject.entity.User;
import com.example.diplomproject.enums.OrderStatus;
import com.example.diplomproject.mapper.PaymentMapper;
import com.example.diplomproject.service.OrderService;
import com.example.diplomproject.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Controller
@RequestMapping("/payment")
public class PaymentController {

    private final PaymentService paymentService;
    private final OrderService orderService;
    private final PaymentMapper paymentMapper;

    @Autowired
    public PaymentController(PaymentService paymentService,
                             OrderService orderService,
                             PaymentMapper paymentMapper) {
        this.paymentService = paymentService;
        this.orderService = orderService;
        this.paymentMapper = paymentMapper;
    }

    @GetMapping("/order/{orderId}")
    public String showPaymentPage(@PathVariable Long orderId,
                                  @AuthenticationPrincipal User currentUser,
                                  Model model) {
        Order order = orderService.getOrderById(orderId);
        if (order == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Заказ не найден");
        }
        if (!order.getUser().getId().equals(currentUser.getId()) && !isAdmin(currentUser)) {
            return "error/403";
        }

        Optional<Payment> existingPayment = paymentService.getPaymentByOrder(order);
        Payment payment;
        if (existingPayment.isPresent()) {
            payment = existingPayment.get();
        } else {
            payment = paymentService.createPayment(order, "CARD");
            // Устанавливаем двустороннюю связь
            order.setPayment(payment);
            orderService.updateOrder(order); // предполагается, что есть такой метод
        }
        PaymentDto paymentDto = paymentMapper.toPaymentDto(payment);

        model.addAttribute("payment", paymentDto);
        model.addAttribute("orderId", order.getId());
        model.addAttribute("title", "Оплата заказа №" + order.getId());
        model.addAttribute("content", "pages/payment/page :: payment-content");
        return "layouts/main";
    }

    @PostMapping("/confirm/{paymentId}")
    public String confirmPayment(@PathVariable Long paymentId,
                                 @AuthenticationPrincipal User currentUser) {
        Payment payment = paymentService.getPaymentById(paymentId);
        Order order = payment.getOrder();
        if (!order.getUser().getId().equals(currentUser.getId()) && !isAdmin(currentUser)) {
            return "error/403";
        }
        // Подтверждаем платёж
        paymentService.confirmPayment(paymentId);
        // Обновляем статус заказа
        orderService.updateOrderStatus(order.getId(), OrderStatus.PAID);
        return "redirect:/orders/" + order.getId();
    }

    @PostMapping("/cancel/{paymentId}")
    public String cancelPayment(@PathVariable Long paymentId,
                                @AuthenticationPrincipal User currentUser) {
        Payment payment = paymentService.getPaymentById(paymentId);
        Order order = payment.getOrder();
        if (!order.getUser().getId().equals(currentUser.getId()) && !isAdmin(currentUser)) {
            return "error/403";
        }
        paymentService.cancelPayment(paymentId);
        // При желании можно отменить и сам заказ:
        // orderService.updateOrderStatus(order.getId(), OrderStatus.CANCELLED);
        return "redirect:/orders/" + order.getId();
    }

    private boolean isAdmin(User user) {
        return user != null && user.getRole() != null && "ADMIN".equalsIgnoreCase(user.getRole().name());
    }
}