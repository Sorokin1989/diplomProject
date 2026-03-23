package com.example.diplomproject.controller;

import com.example.diplomproject.entity.Course;
import com.example.diplomproject.entity.Order;
import com.example.diplomproject.entity.OrderItem;
import com.example.diplomproject.entity.User;
import com.example.diplomproject.enums.OrderStatus;
import com.example.diplomproject.service.CartService;
import com.example.diplomproject.service.CourseService;
import com.example.diplomproject.service.OrderService;
import com.example.diplomproject.service.PromocodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/orders")
public class OrderController {


    private CartService cartService;//для получения стоимости
    private PromocodeService promocodeService;//применить купон
    private OrderService orderService;
    private CourseService courseService; // предположим, что он нужен для формы создания

    @Autowired
    public OrderController(CartService cartService, PromocodeService promocodeService, OrderService orderService, CourseService courseService) {
        this.cartService = cartService;
        this.promocodeService = promocodeService;
        this.orderService = orderService;
        this.courseService = courseService;
    }

    /**
     * Список заказов.
     * Для администратора – все заказы, для обычного пользователя – только его.
     */
    @GetMapping
    public String listOrders(@AuthenticationPrincipal User currentUser, Model model) {
        List<Order> orders;
        if (isAdmin(currentUser)) {
            orders = orderService.getAllOrders();
        } else {
            orders = orderService.getOrdersByUser(currentUser);
        }
        model.addAttribute("orders", orders);
        model.addAttribute("isAdmin", isAdmin(currentUser));
        return "orders/list";
    }

    /**
     * Детальный просмотр заказа.
     * Доступ: администратор или владелец заказа.
     */
    @GetMapping("/{id}")
    public String viewOrder(@PathVariable Long id,
                            @AuthenticationPrincipal User currentUser,
                            Model model) {
        Order order = orderService.getOrderById(id);
        // Проверка прав
        if (!isAdmin(currentUser) && !order.getUser().getId().equals(currentUser.getId())) {
            return "error/403";
        }
        model.addAttribute("order", order);
        model.addAttribute("isAdmin", isAdmin(currentUser));
        model.addAttribute("availableStatuses", OrderStatus.values());
        return "orders/view";
    }

    /**
     * Изменение статуса заказа (только для администратора).
     * Обработка POST-запроса из формы.
     */
    @PostMapping("/{id}/status")
    public String updateOrderStatus(@PathVariable Long id,
                                    @RequestParam OrderStatus status,
                                    @AuthenticationPrincipal User currentUser) {
        if (!isAdmin(currentUser)) {
            return "error/403";
        }
        orderService.updateOrderStatus(id, status);
        return "redirect:/orders/" + id;
    }

    /**
     * Отмена заказа (доступно для владельца, если заказ ещё не обработан).
     */
    @PostMapping("/{id}/cancel")
    public String cancelOrder(@PathVariable Long id,
                              @AuthenticationPrincipal User currentUser) {
        Order order = orderService.getOrderById(id);
        if (!order.getUser().getId().equals(currentUser.getId())) {
            return "error/403";
        }
        // Можно добавить проверку: отменять можно только в статусе PENDING
        if (order.getOrderStatus() == OrderStatus.PENDING) {
            orderService.updateOrderStatus(id, OrderStatus.CANCELLED);
        }
        return "redirect:/orders/" + id;
    }

    /**
     * Форма создания нового заказа (выбор курсов).
     */
    @GetMapping("/create")
    public String showCreateForm(Model model, @AuthenticationPrincipal User currentUser) {
        List<Course> availableCourses = courseService.getAllCourses(); // все доступные курсы
        model.addAttribute("courses", availableCourses);
        model.addAttribute("orderRequest", new OrderRequest());
        return "orders/create";
    }

    /**
     * Обработка создания заказа.
     */
    @PostMapping("/create")
    public String createOrder(@ModelAttribute OrderRequest orderRequest,
                              @AuthenticationPrincipal User currentUser) {
        // Преобразуем OrderRequest в список OrderItem
        List<OrderItem> orderItems = new ArrayList<>();
        for (OrderRequest.CourseOrderItem item : orderRequest.getItems()) {
            if (item.getQuantity() > 0) {
                Course course = courseService.getCourseById(item.getCourseId());
                OrderItem orderItem = new OrderItem();
                orderItem.setCourse(course);
                orderItem.setQuantity(item.getQuantity());
                orderItem.setPrice(course.getPrice()); // цена на момент заказа
                orderItems.add(orderItem);
            }
        }
        if (orderItems.isEmpty()) {
            throw new IllegalArgumentException("Выберите хотя бы один курс");
        }
        Order order = orderService.createOrder(currentUser, orderItems);
        return "redirect:/orders/" + order.getId();
    }
//метод для применения промокода при оформлении заказа
    @PostMapping("/apply-promocode")
    public String applyPromocode(@RequestParam String promocode,
                                 @AuthenticationPrincipal User user,
                                 Model model) {
        try {
            BigDecimal currentTotal = cartService.getTotalPrice(user);
            BigDecimal discountedTotal = promocodeService.applyPromocode(currentTotal, promocode);
            // Сохраняем применённый промокод в сессии или временно для текущего заказа
            // Например, в сессионный атрибут
            // session.setAttribute("appliedPromocode", promocode);
            // session.setAttribute("discountedTotal", discountedTotal);
            model.addAttribute("discountedTotal", discountedTotal);
            model.addAttribute("promocodeApplied", true);
        } catch (IllegalArgumentException e) {
            model.addAttribute("promocodeError", e.getMessage());
        }
        // Вернуться на страницу корзины/оформления
        return "pages/cart/checkout";
    }

    // Вспомогательный метод для проверки роли администратора
    private boolean isAdmin(User user) {
        return user != null && user.getRole() != null && "ADMIN".equalsIgnoreCase(String.valueOf(user.getRole()));
    }
}

/**
 * DTO для формы создания заказа.
 */
class OrderRequest {
    private List<CourseOrderItem> items = new ArrayList<>();

    public List<CourseOrderItem> getItems() {
        return items;
    }

    public void setItems(List<CourseOrderItem> items) {
        this.items = items;
    }

    public static class CourseOrderItem {
        private Long courseId;
        private int quantity;

        public Long getCourseId() {
            return courseId;
        }

        public void setCourseId(Long courseId) {
            this.courseId = courseId;
        }

        public int getQuantity() {
            return quantity;
        }

        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }
    }
}