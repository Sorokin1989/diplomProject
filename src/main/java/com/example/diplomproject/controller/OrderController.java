package com.example.diplomproject.controller;

import com.example.diplomproject.dto.CartDto;
import com.example.diplomproject.dto.CourseDto;
import com.example.diplomproject.dto.OrderDto;
import com.example.diplomproject.entity.Course;
import com.example.diplomproject.entity.Order;
import com.example.diplomproject.entity.OrderItem;
import com.example.diplomproject.entity.User;
import com.example.diplomproject.enums.OrderStatus;
import com.example.diplomproject.mapper.CourseMapper;
import com.example.diplomproject.mapper.OrderMapper;
import com.example.diplomproject.service.CartService;
import com.example.diplomproject.service.CourseService;
import com.example.diplomproject.service.OrderService;
import com.example.diplomproject.service.PromocodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/orders")
public class OrderController {

    private final CartService cartService;
    private final PromocodeService promocodeService;
    private final OrderService orderService;
    private final CourseService courseService;
    private final OrderMapper orderMapper;
    private final CourseMapper courseMapper;

    @Autowired
    public OrderController(CartService cartService,
                           PromocodeService promocodeService,
                           OrderService orderService,
                           CourseService courseService,
                           OrderMapper orderMapper,
                           CourseMapper courseMapper) {
        this.cartService = cartService;
        this.promocodeService = promocodeService;
        this.orderService = orderService;
        this.courseService = courseService;
        this.orderMapper = orderMapper;
        this.courseMapper = courseMapper;
    }

    @GetMapping
    public String listOrders(@AuthenticationPrincipal User currentUser, Model model) {
        List<Order> orders;
        if (isAdmin(currentUser)) {
            orders = orderService.getAllOrders();
        } else {
            orders = orderService.getOrdersByUser(currentUser);
        }
        List<OrderDto> orderDtos = orders.stream()
                .map(orderMapper::toOrderDTO)
                .collect(Collectors.toList());

        model.addAttribute("orders", orderDtos);
        model.addAttribute("isAdmin", isAdmin(currentUser));
        model.addAttribute("title", "Мои заказы");
        model.addAttribute("content", "pages/orders/list :: orders-list-content");
        return "layouts/main";
    }

    @GetMapping("/{id}")
    public String viewOrder(@PathVariable Long id,
                            @AuthenticationPrincipal User currentUser,
                            Model model) {
        Order order = orderService.getOrderById(id);
        if (order == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Заказ не найден");
        }
        if (!isAdmin(currentUser) && !order.getUser().getId().equals(currentUser.getId())) {
            return "error/403";
        }
        OrderDto orderDto = orderMapper.toOrderDTO(order);
        model.addAttribute("order", orderDto);
        model.addAttribute("isAdmin", isAdmin(currentUser));
        model.addAttribute("availableStatuses", OrderStatus.values());
        model.addAttribute("title", "Заказ №" + orderDto.getId());
        model.addAttribute("content", "pages/orders/detail :: order-detail-content");
        return "layouts/main";
    }

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

    @PostMapping("/{id}/cancel")
    public String cancelOrder(@PathVariable Long id,
                              @AuthenticationPrincipal User currentUser) {
        Order order = orderService.getOrderById(id);
        if (order == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Заказ не найден");
        }
        if (!order.getUser().getId().equals(currentUser.getId())) {
            return "error/403";
        }
        if (order.getOrderStatus() == OrderStatus.PENDING) {
            orderService.updateOrderStatus(id, OrderStatus.CANCELLED);
        }
        return "redirect:/orders/" + id;
    }

    @GetMapping("/create")
    public String showCreateForm(Model model, @AuthenticationPrincipal User currentUser) {
        List<Course> availableCourses = courseService.getAllCourses();
        List<CourseDto> courseDtos = availableCourses.stream()
                .map(courseMapper::toCourseDto)
                .collect(Collectors.toList());
        model.addAttribute("courses", courseDtos);
        model.addAttribute("orderRequest", new OrderRequest());
        model.addAttribute("title", "Создание заказа");
        model.addAttribute("content", "pages/orders/create :: order-create-content");
        return "layouts/main";
    }

    @PostMapping("/create")
    public String createOrder(@ModelAttribute OrderRequest orderRequest,
                              @AuthenticationPrincipal User currentUser) {
        List<OrderItem> orderItems = new ArrayList<>();
        for (OrderRequest.CourseOrderItem item : orderRequest.getItems()) {
            if (item.getQuantity() > 0) {
                Course course = courseService.getCourseById(item.getCourseId());
                if (course == null) {
                    throw new IllegalArgumentException("Курс с id=" + item.getCourseId() + " не найден");
                }
                OrderItem orderItem = new OrderItem();
                orderItem.setCourse(course);
                orderItem.setQuantity(item.getQuantity());
                orderItem.setPrice(course.getPrice());
                orderItems.add(orderItem);
            }
        }
        if (orderItems.isEmpty()) {
            throw new IllegalArgumentException("Выберите хотя бы один курс");
        }
        Order order = orderService.createOrder(currentUser, orderItems);
        return "redirect:/orders/" + order.getId();
    }

    /**
     * Страница оформления заказа (отображение корзины + применение промокода)
     */
    @GetMapping("/checkout")
    public String showCheckoutPage(@AuthenticationPrincipal User user, Model model) {
        // Получаем DTO корзины для отображения
        CartDto cartDto = cartService.getOrCreateCartDto(user);
        BigDecimal total = cartService.getTotalPriceDto(user);

        model.addAttribute("cartItems", cartDto.getCartItems());
        model.addAttribute("totalPrice", total);
        model.addAttribute("title", "Оформление заказа");
        model.addAttribute("content", "pages/orders/checkout :: checkout-content");
        return "layouts/main";
    }


    @PostMapping("/apply-promocode")
    public String applyPromocode(@RequestParam String promocode,
                                 @AuthenticationPrincipal User user,
                                 Model model) {
        try {
            BigDecimal currentTotal = cartService.getTotalPriceDto(user);
            BigDecimal discountedTotal = promocodeService.applyPromocode(currentTotal, promocode);
            model.addAttribute("discountedTotal", discountedTotal);
            model.addAttribute("promocodeApplied", true);
        } catch (IllegalArgumentException e) {
            model.addAttribute("promocodeError", e.getMessage());
        }

        // Повторно загружаем корзину для отображения
        CartDto cartDto = cartService.getOrCreateCartDto(user);
        model.addAttribute("cartItems", cartDto.getCartItems());
        model.addAttribute("totalPrice", cartService.getTotalPriceDto(user));
        model.addAttribute("title", "Оформление заказа");
        model.addAttribute("content", "pages/orders/checkout :: checkout-content");
        return "layouts/main";
    }

    private boolean isAdmin(User user) {
        return user != null && user.getRole() != null && "ADMIN".equalsIgnoreCase(String.valueOf(user.getRole()));
    }
}

// DTO для формы создания заказа
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