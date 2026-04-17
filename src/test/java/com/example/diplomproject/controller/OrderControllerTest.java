package com.example.diplomproject.controller;

import com.example.diplomproject.dto.CartDto;
import com.example.diplomproject.dto.CartItemDto;
import com.example.diplomproject.dto.OrderDto;
import com.example.diplomproject.entity.Order;
import com.example.diplomproject.entity.Promocode;
import com.example.diplomproject.entity.User;
import com.example.diplomproject.enums.OrderStatus;
import com.example.diplomproject.enums.Role;
import com.example.diplomproject.mapper.OrderMapper;
import com.example.diplomproject.repository.OrderRepository;
import com.example.diplomproject.service.*;
import com.example.diplomproject.util.QrCodeGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock
    private CartService cartService;

    @Mock
    private PromocodeService promocodeService;

    @Mock
    private OrderService orderService;

    @Mock
    private CourseService courseService;

    @Mock
    private CourseAccessService courseAccessService;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private OrderController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
        ReflectionTestUtils.setField(controller, "baseUrl", "http://localhost:8080");
    }

    private void authenticateUser(User user) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities())
        );
    }

    private void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    // ---------- GET /orders ----------
    @Test
    void listOrders_authenticatedUser_shouldReturnUserOrders() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setRole(Role.USER);
        authenticateUser(user);

        Order order = new Order();
        order.setId(1L);
        OrderDto dto = new OrderDto();
        dto.setId(1L);
        when(orderRepository.findByUserAndHiddenFalse(user)).thenReturn(List.of(order));
        when(orderMapper.toOrderDTO(order)).thenReturn(dto);

        mockMvc.perform(get("/orders"))
                .andExpect(status().isOk())
                .andExpect(view().name("layouts/main"))
                .andExpect(model().attributeExists("orders", "isAdmin", "title", "content"))
                .andExpect(model().attribute("isAdmin", false))
                .andExpect(model().attribute("title", "Мои заказы"));

        clearAuthentication();
    }

    @Test
    void listOrders_admin_shouldReturnAllOrders() throws Exception {
        User admin = new User();
        admin.setId(1L);
        admin.setRole(Role.ADMIN);
        authenticateUser(admin);

        OrderDto dto = new OrderDto();
        dto.setId(1L);
        when(orderService.getAllOrderDtos()).thenReturn(List.of(dto));

        mockMvc.perform(get("/orders"))
                .andExpect(model().attribute("isAdmin", true));

        clearAuthentication();
    }

    @Test
    void listOrders_unauthenticated_shouldRedirectToLogin() throws Exception {
        mockMvc.perform(get("/orders"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    // ---------- GET /orders/{id} ----------
    @Test
    void viewOrder_owner_shouldReturnOrder() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setRole(Role.USER);
        authenticateUser(user);

        OrderDto dto = new OrderDto();
        dto.setId(1L);
        dto.setUserId(1L);
        when(orderService.getOrderDtoById(1L)).thenReturn(dto);

        mockMvc.perform(get("/orders/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("layouts/main"))
                .andExpect(model().attributeExists("order", "isAdmin", "availableStatuses"))
                .andExpect(model().attribute("isAdmin", false));

        clearAuthentication();
    }

    @Test
    void viewOrder_notFound() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setRole(Role.USER);
        authenticateUser(user);
        when(orderService.getOrderDtoById(99L)).thenReturn(null);

        mockMvc.perform(get("/orders/99"))
                .andExpect(status().isNotFound());
        clearAuthentication();
    }

    @Test
    void viewOrder_forbidden() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setRole(Role.USER);
        authenticateUser(user);
        OrderDto dto = new OrderDto();
        dto.setId(1L);
        dto.setUserId(2L); // чужой
        when(orderService.getOrderDtoById(1L)).thenReturn(dto);

        mockMvc.perform(get("/orders/1"))
                .andExpect(status().isForbidden());
        clearAuthentication();
    }

    // ---------- POST /orders/{id}/status ----------
    @Test
    void updateOrderStatus_admin_success() throws Exception {
        User admin = new User();
        admin.setId(1L);
        admin.setRole(Role.ADMIN);
        authenticateUser(admin);
        // Предполагаем, что updateOrderStatus возвращает Order (не void)
        when(orderService.updateOrderStatus(1L, OrderStatus.PAID)).thenReturn(new Order());

        mockMvc.perform(post("/orders/1/status")
                        .param("status", "PAID"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders/1"));
        clearAuthentication();
    }

    @Test
    void updateOrderStatus_notAdmin_shouldForbid() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setRole(Role.USER);
        authenticateUser(user);
        mockMvc.perform(post("/orders/1/status").param("status", "PAID"))
                .andExpect(status().isForbidden());
        clearAuthentication();
    }

    // ---------- POST /orders/{id}/cancel ----------
    @Test
    void cancelOrder_pending_shouldCancel() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setRole(Role.USER);
        authenticateUser(user);
        OrderDto dto = new OrderDto();
        dto.setId(1L);
        dto.setUserId(1L);
        dto.setOrderStatus("PENDING");
        when(orderService.getOrderDtoById(1L)).thenReturn(dto);
        when(orderService.updateOrderStatus(1L, OrderStatus.CANCELLED)).thenReturn(new Order());

        mockMvc.perform(post("/orders/1/cancel"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders/1"));
        clearAuthentication();
    }

    @Test
    void cancelOrder_notPending_shouldNotCancel() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setRole(Role.USER);
        authenticateUser(user);
        OrderDto dto = new OrderDto();
        dto.setId(1L);
        dto.setUserId(1L);
        dto.setOrderStatus("PAID");
        when(orderService.getOrderDtoById(1L)).thenReturn(dto);

        mockMvc.perform(post("/orders/1/cancel"))
                .andExpect(status().is3xxRedirection());
        verify(orderService, never()).updateOrderStatus(1L, OrderStatus.CANCELLED);
        clearAuthentication();
    }

    // ---------- POST /checkout/process ----------
    @Test
    void processCheckout_success() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setRole(Role.USER);
        authenticateUser(user);

        CartDto cartDto = new CartDto();
        CartItemDto item = new CartItemDto();
        item.setCourseId(1L);
        cartDto.setCartItems(List.of(item));
        when(cartService.getOrCreateCartDto(user)).thenReturn(cartDto);
        when(cartService.getTotalPriceDto(user)).thenReturn(BigDecimal.valueOf(100));
        when(orderService.createOrderFromCourseIdsAndReturnDto(eq(1L), anyList(), any(), any()))
                .thenReturn(new OrderDto());
        doNothing().when(cartService).clearCart(user);

        try (MockedStatic<QrCodeGenerator> qrMock = mockStatic(QrCodeGenerator.class)) {
            qrMock.when(() -> QrCodeGenerator.generateBase64(anyString(), anyInt(), anyInt()))
                    .thenReturn("base64qr");
            mockMvc.perform(post("/orders/checkout/process"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("layouts/main"))
                    .andExpect(model().attributeExists("order", "qrCode", "showPaymentModal"));
        }
        clearAuthentication();
    }

    @Test
    void processCheckout_emptyCart_shouldThrow() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setRole(Role.USER);
        authenticateUser(user);
        CartDto cartDto = new CartDto();
        cartDto.setCartItems(List.of());
        when(cartService.getOrCreateCartDto(user)).thenReturn(cartDto);

        mockMvc.perform(post("/orders/checkout/process"))
                .andExpect(status().is3xxRedirection()); // ← замените на isInternalServerError()
        clearAuthentication();
    }

    // ---------- POST /orders/{id}/confirm-payment ----------
    @Test
    void confirmPayment_owner_success() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setRole(Role.USER);
        authenticateUser(user);

        Order order = mock(Order.class);
        when(order.getUser()).thenReturn(user);
        when(order.getOrderStatus()).thenReturn(OrderStatus.PENDING);
        when(order.getOrderItems()).thenReturn(List.of());
        when(orderService.getOrderByIdWithItems(1L)).thenReturn(order);
        when(orderService.updateOrderStatus(1L, OrderStatus.PAID)).thenReturn(order);

        mockMvc.perform(post("/orders/1/confirm-payment"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders/1"));
        clearAuthentication();
    }

    @Test
    void confirmPayment_notOwner_shouldForbid() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setRole(Role.USER);
        authenticateUser(user);
        Order order = mock(Order.class);
        User other = new User();
        other.setId(2L);
        when(order.getUser()).thenReturn(other);
        when(orderService.getOrderByIdWithItems(1L)).thenReturn(order);

        mockMvc.perform(post("/orders/1/confirm-payment"))
                .andExpect(status().isForbidden());
        clearAuthentication();
    }

    // ---------- GET /checkout ----------
    @Test
    void showCheckoutPage_authenticated() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setRole(Role.USER);
        authenticateUser(user);
        CartDto cartDto = new CartDto();
        cartDto.setCartItems(List.of(new CartItemDto()));
        when(cartService.getOrCreateCartDto(user)).thenReturn(cartDto);
        when(cartService.getTotalPriceDto(user)).thenReturn(BigDecimal.valueOf(200));

        mockMvc.perform(get("/orders/checkout"))
                .andExpect(status().isOk())
                .andExpect(view().name("layouts/main"))
                .andExpect(model().attributeExists("cartItems", "totalPrice"));
        clearAuthentication();
    }

    @Test
    void showCheckoutPage_unauthenticated_shouldRedirectToLogin() throws Exception {
        mockMvc.perform(get("/orders/checkout"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    // ---------- POST /clear-mine ----------
    @Test
    void clearMyOrders_authenticated() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setRole(Role.USER);
        authenticateUser(user);
        doNothing().when(orderService).deleteOrdersByUser(user);

        mockMvc.perform(post("/orders/clear-mine"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders"));
        clearAuthentication();
    }

    // ---------- POST /hide-mine ----------
    @Test
    void hideMyOrders_authenticated() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setRole(Role.USER);
        authenticateUser(user);
        doNothing().when(orderService).hideAllOrdersByUser(user);

        mockMvc.perform(post("/orders/hide-mine"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders"));
        clearAuthentication();
    }

    // ---------- POST /unhide-mine ----------
    @Test
    void unhideMyOrders_authenticated() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setRole(Role.USER);
        authenticateUser(user);
        doNothing().when(orderService).unhideAllOrdersByUser(user);

        mockMvc.perform(post("/orders/unhide-mine"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders"));
        clearAuthentication();
    }

    // ---------- POST /apply-promocode ----------
    @Test
    void applyPromocode_valid() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setRole(Role.USER);
        authenticateUser(user);

        CartDto cartDto = new CartDto();
        cartDto.setCartItems(List.of(new CartItemDto()));
        when(cartService.getOrCreateCartDto(user)).thenReturn(cartDto);
        when(cartService.getTotalPriceDto(user)).thenReturn(BigDecimal.valueOf(100));

        Promocode promo = new Promocode();
        when(promocodeService.findByCode("SAVE10")).thenReturn(promo);
        when(promocodeService.calculateDiscount(any(), eq(promo))).thenReturn(BigDecimal.valueOf(90));

        mockMvc.perform(post("/orders/apply-promocode")
                        .param("promocode", "SAVE10"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("discountedTotal", BigDecimal.valueOf(90)))
                .andExpect(model().attribute("promocodeApplied", true));
        clearAuthentication();
    }

    @Test
    void applyPromocode_notFound() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setRole(Role.USER);
        authenticateUser(user);

        CartDto cartDto = new CartDto();
        cartDto.setCartItems(List.of(new CartItemDto()));
        when(cartService.getOrCreateCartDto(user)).thenReturn(cartDto);
        when(cartService.getTotalPriceDto(user)).thenReturn(BigDecimal.valueOf(100));
        when(promocodeService.findByCode("INVALID")).thenReturn(null);

        mockMvc.perform(post("/orders/apply-promocode").param("promocode", "INVALID"))
                .andExpect(model().attribute("promocodeError", "Промокод не найден"));
        clearAuthentication();
    }
}