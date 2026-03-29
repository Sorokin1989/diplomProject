package com.example.diplomproject.controller;
import com.example.diplomproject.enums.OrderStatus;
import com.example.diplomproject.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequestMapping("/admin/orders")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;

    @GetMapping
    public String listOrders(Model model) {
        model.addAttribute("orders", orderService.getAllOrders());
        model.addAttribute("statuses", OrderStatus.values());
        model.addAttribute("title", "Управление заказами");
        model.addAttribute("content", "pages/admin/orders/admin-list :: admin-orders-content");
        return "layouts/main";
    }

    @PostMapping("/{id}/status")
    public String updateOrderStatus(@PathVariable Long id,
                                    @RequestParam OrderStatus status,
                                    RedirectAttributes redirectAttributes) {
        try {
            orderService.updateOrderStatus(id, status);
            redirectAttributes.addAttribute("success", "Статус заказа обновлён");
        } catch (Exception e) {
            log.error("Ошибка обновления статуса заказа {}", id, e);
            redirectAttributes.addAttribute("error", "Ошибка обновления: " + e.getMessage());
        }
        return "redirect:/admin/orders";
    }
}