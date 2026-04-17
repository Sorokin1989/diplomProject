package com.example.diplomproject.controller;

import com.example.diplomproject.enums.OrderStatus;
import com.example.diplomproject.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AdminOrderControllerTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private AdminOrderController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    // ---------- GET /admin/orders ----------
    @Test
    void listOrders_shouldReturnViewWithOrdersAndStatuses() throws Exception {
        when(orderService.getAllOrders()).thenReturn(List.of());

        mockMvc.perform(get("/admin/orders"))
                .andExpect(status().isOk())
                .andExpect(view().name("layouts/main"))
                .andExpect(model().attributeExists("orders", "statuses"))
                .andExpect(model().attribute("title", "Управление заказами"))
                .andExpect(model().attribute("content", "pages/admin/orders/admin-list :: admin-orders-content"));

        verify(orderService).getAllOrders();
    }

    // ---------- POST /admin/orders/{id}/status ----------
    @Test
    void updateOrderStatus_success() throws Exception {
        // Предполагаем, что updateOrderStatus возвращает Order (или любой объект)
        when(orderService.updateOrderStatus(1L, OrderStatus.PAID)).thenReturn(null);

        mockMvc.perform(post("/admin/orders/1/status")
                        .param("status", "PAID"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/admin/orders*"));

        verify(orderService).updateOrderStatus(1L, OrderStatus.PAID);
    }

    @Test
    void updateOrderStatus_failure() throws Exception {
        when(orderService.updateOrderStatus(1L, OrderStatus.PAID))
                .thenThrow(new RuntimeException("Update failed"));

        mockMvc.perform(post("/admin/orders/1/status")
                        .param("status", "PAID"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/admin/orders*"));

        verify(orderService).updateOrderStatus(1L, OrderStatus.PAID);
    }
}