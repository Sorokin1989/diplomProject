package com.example.diplomproject.controller;

import com.example.diplomproject.dto.PaymentDto;
import com.example.diplomproject.entity.Order;
import com.example.diplomproject.entity.Payment;
import com.example.diplomproject.entity.User;
import com.example.diplomproject.enums.OrderStatus;
import com.example.diplomproject.enums.Role;
import com.example.diplomproject.mapper.PaymentMapper;
import com.example.diplomproject.service.OrderService;
import com.example.diplomproject.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    @Mock
    private PaymentService paymentService;

    @Mock
    private OrderService orderService;

    @Mock
    private PaymentMapper paymentMapper;

    @InjectMocks
    private PaymentController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    private void authenticateUser(User user) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities())
        );
    }

    private void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    // ---------- GET /payment/order/{orderId} ----------
    @Test
    void showPaymentPage_owner_shouldReturnPage() throws Exception {
        User owner = new User();
        owner.setId(1L);
        owner.setRole(Role.USER);
        authenticateUser(owner);

        Order order = new Order();
        order.setId(1L);
        order.setUser(owner);
        when(orderService.getOrderById(1L)).thenReturn(order);

        Payment payment = new Payment();
        payment.setId(10L);
        when(paymentService.getPaymentByOrder(order)).thenReturn(Optional.of(payment));
        PaymentDto dto = new PaymentDto();
        when(paymentMapper.toPaymentDto(payment)).thenReturn(dto);

        mockMvc.perform(get("/payment/order/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("layouts/main"))
                .andExpect(model().attributeExists("payment", "orderId", "title", "content"))
                .andExpect(model().attribute("orderId", 1L));

        clearAuthentication();
    }

    @Test
    void showPaymentPage_admin_shouldReturnPage() throws Exception {
        User admin = new User();
        admin.setId(2L);
        admin.setRole(Role.ADMIN);
        authenticateUser(admin);

        User owner = new User();
        owner.setId(1L);
        Order order = new Order();
        order.setId(1L);
        order.setUser(owner);
        when(orderService.getOrderById(1L)).thenReturn(order);

        Payment payment = new Payment();
        payment.setId(10L);
        when(paymentService.getPaymentByOrder(order)).thenReturn(Optional.of(payment));
        PaymentDto dto = new PaymentDto();
        when(paymentMapper.toPaymentDto(payment)).thenReturn(dto);

        mockMvc.perform(get("/payment/order/1"))
                .andExpect(status().isOk());

        clearAuthentication();
    }

    @Test
    void showPaymentPage_orderNotFound_shouldReturn404() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setRole(Role.USER);
        authenticateUser(user);
        when(orderService.getOrderById(99L)).thenReturn(null);

        mockMvc.perform(get("/payment/order/99"))
                .andExpect(status().isNotFound());
        clearAuthentication();
    }

    @Test
    void showPaymentPage_forbidden_shouldReturn403View() throws Exception {
        User stranger = new User();
        stranger.setId(2L);
        stranger.setRole(Role.USER);
        authenticateUser(stranger);

        User owner = new User();
        owner.setId(1L);
        Order order = new Order();
        order.setId(1L);
        order.setUser(owner);
        when(orderService.getOrderById(1L)).thenReturn(order);

        mockMvc.perform(get("/payment/order/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("error/403"));
        clearAuthentication();
    }

    @Test
    void showPaymentPage_createNewPayment() throws Exception {
        User owner = new User();
        owner.setId(1L);
        owner.setRole(Role.USER);
        authenticateUser(owner);

        Order order = new Order();
        order.setId(1L);
        order.setUser(owner);
        when(orderService.getOrderById(1L)).thenReturn(order);
        when(paymentService.getPaymentByOrder(order)).thenReturn(Optional.empty());

        Payment newPayment = new Payment();
        newPayment.setId(10L);
        when(paymentService.createPayment(order, "CARD")).thenReturn(newPayment);
        when(orderService.updateOrder(order)).thenReturn(order);
        PaymentDto dto = new PaymentDto();
        when(paymentMapper.toPaymentDto(newPayment)).thenReturn(dto);

        mockMvc.perform(get("/payment/order/1"))
                .andExpect(status().isOk());

        verify(paymentService).createPayment(order, "CARD");
        verify(orderService).updateOrder(order);
        clearAuthentication();
    }

    // ---------- POST /payment/confirm/{paymentId} ----------
    @Test
    void confirmPayment_owner_success() throws Exception {
        User owner = new User();
        owner.setId(1L);
        owner.setRole(Role.USER);
        authenticateUser(owner);

        Payment payment = new Payment();
        payment.setId(10L);
        Order order = new Order();
        order.setId(1L);
        order.setUser(owner);
        payment.setOrder(order);
        when(paymentService.getPaymentById(10L)).thenReturn(payment);
        when(paymentService.confirmPayment(10L)).thenReturn(payment);
        when(orderService.updateOrderStatus(1L, OrderStatus.PAID)).thenReturn(order);

        mockMvc.perform(post("/payment/confirm/10"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders/1"));

        verify(paymentService).confirmPayment(10L);
        verify(orderService).updateOrderStatus(1L, OrderStatus.PAID);
        clearAuthentication();
    }

    @Test
    void confirmPayment_admin_success() throws Exception {
        User admin = new User();
        admin.setId(2L);
        admin.setRole(Role.ADMIN);
        authenticateUser(admin);

        Payment payment = new Payment();
        payment.setId(10L);
        Order order = new Order();
        order.setId(1L);
        User owner = new User();
        owner.setId(1L);
        order.setUser(owner);
        payment.setOrder(order);
        when(paymentService.getPaymentById(10L)).thenReturn(payment);
        when(paymentService.confirmPayment(10L)).thenReturn(payment);
        when(orderService.updateOrderStatus(1L, OrderStatus.PAID)).thenReturn(order);

        mockMvc.perform(post("/payment/confirm/10"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders/1"));
        clearAuthentication();
    }

    @Test
    void confirmPayment_forbidden_shouldReturn403View() throws Exception {
        User stranger = new User();
        stranger.setId(2L);
        stranger.setRole(Role.USER);
        authenticateUser(stranger);

        Payment payment = new Payment();
        payment.setId(10L);
        Order order = new Order();
        order.setId(1L);
        User owner = new User();
        owner.setId(1L);
        order.setUser(owner);
        payment.setOrder(order);
        when(paymentService.getPaymentById(10L)).thenReturn(payment);

        mockMvc.perform(post("/payment/confirm/10"))
                .andExpect(status().isOk())
                .andExpect(view().name("error/403"));
        clearAuthentication();
    }

    // ---------- POST /payment/cancel/{paymentId} ----------
    @Test
    void cancelPayment_owner_success() throws Exception {
        User owner = new User();
        owner.setId(1L);
        owner.setRole(Role.USER);
        authenticateUser(owner);

        Payment payment = new Payment();
        payment.setId(10L);
        Order order = new Order();
        order.setId(1L);
        order.setUser(owner);
        payment.setOrder(order);
        when(paymentService.getPaymentById(10L)).thenReturn(payment);
        when(paymentService.cancelPayment(10L)).thenReturn(payment);

        mockMvc.perform(post("/payment/cancel/10"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders/1"));

        verify(paymentService).cancelPayment(10L);
        clearAuthentication();
    }

    @Test
    void cancelPayment_admin_success() throws Exception {
        User admin = new User();
        admin.setId(2L);
        admin.setRole(Role.ADMIN);
        authenticateUser(admin);

        Payment payment = new Payment();
        payment.setId(10L);
        Order order = new Order();
        order.setId(1L);
        User owner = new User();
        owner.setId(1L);
        order.setUser(owner);
        payment.setOrder(order);
        when(paymentService.getPaymentById(10L)).thenReturn(payment);
        when(paymentService.cancelPayment(10L)).thenReturn(payment);

        mockMvc.perform(post("/payment/cancel/10"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders/1"));
        clearAuthentication();
    }

    @Test
    void cancelPayment_forbidden_shouldReturn403View() throws Exception {
        User stranger = new User();
        stranger.setId(2L);
        stranger.setRole(Role.USER);
        authenticateUser(stranger);

        Payment payment = new Payment();
        payment.setId(10L);
        Order order = new Order();
        order.setId(1L);
        User owner = new User();
        owner.setId(1L);
        order.setUser(owner);
        payment.setOrder(order);
        when(paymentService.getPaymentById(10L)).thenReturn(payment);

        mockMvc.perform(post("/payment/cancel/10"))
                .andExpect(status().isOk())
                .andExpect(view().name("error/403"));
        clearAuthentication();
    }
}