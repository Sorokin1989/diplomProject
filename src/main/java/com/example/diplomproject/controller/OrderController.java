package com.example.diplomproject.controller;

import com.example.diplomproject.dto.CartItemDto;
import com.example.diplomproject.dto.CartDto;
import com.example.diplomproject.dto.OrderDto;
import com.example.diplomproject.entity.Order;
import com.example.diplomproject.entity.OrderItem;
import com.example.diplomproject.entity.Promocode;
import com.example.diplomproject.entity.User;
import com.example.diplomproject.enums.OrderStatus;
import com.example.diplomproject.enums.Role;
import com.example.diplomproject.mapper.OrderMapper;
import com.example.diplomproject.repository.OrderRepository;
import com.example.diplomproject.service.*;
import com.example.diplomproject.util.QrCodeGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/orders")
public class OrderController {

    private final CartService cartService;
    private final PromocodeService promocodeService;
    private final OrderService orderService;
    private final CourseService courseService;
    private final CourseAccessService courseAccessService;
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;

    @Autowired
    public OrderController(CartService cartService,
                           PromocodeService promocodeService,
                           OrderService orderService,
                           CourseService courseService,
                           CourseAccessService courseAccessService, OrderRepository orderRepository, OrderMapper orderMapper) {
        this.cartService = cartService;
        this.promocodeService = promocodeService;
        this.orderService = orderService;
        this.courseService = courseService;
        this.courseAccessService = courseAccessService;
        this.orderRepository = orderRepository;
        this.orderMapper = orderMapper;
    }

    @GetMapping
    public String listOrders(@AuthenticationPrincipal User currentUser, Model model) {
        List<OrderDto> orderDtos;
        if (isAdmin(currentUser)) {
            orderDtos = orderService.getAllOrderDtos();
        } else {
            List<Order> orders = orderRepository.findByUserAndHiddenFalse(currentUser);
            orderDtos = orders.stream().map(orderMapper::toOrderDTO).collect(Collectors.toList());
        }
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
        boolean isAdmin = currentUser != null && currentUser.getRole() == Role.ADMIN;
        OrderDto orderDto = orderService.getOrderDtoById(id);
        if (orderDto == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Заказ не найден");
        }
        if (!isAdmin(currentUser) && !orderDto.getUserId().equals(currentUser.getId())) {
            return "error/403";
        }

        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("order", orderDto);
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
        OrderDto orderDto = orderService.getOrderDtoById(id);
        if (orderDto == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Заказ не найден");
        }
        if (!orderDto.getUserId().equals(currentUser.getId())) {
            return "error/403";
        }
        if ("PENDING".equals(orderDto.getOrderStatus())) {
            orderService.updateOrderStatus(id, OrderStatus.CANCELLED);
        }
        return "redirect:/orders/" + id;
    }
//
//    @GetMapping("/create")
//    public String showCreateForm(Model model, @AuthenticationPrincipal User currentUser) {
//        // Получаем корзину текущего пользователя
//        CartDto cartDto = cartService.getOrCreateCartDto(currentUser);
//        model.addAttribute("cartItems", cartDto.getCartItems());
//        model.addAttribute("totalPrice", cartService.getTotalPriceDto(currentUser));
//        model.addAttribute("title", "Корзина");
//        model.addAttribute("content", "pages/orders/create :: order-create-content");
//        return "layouts/main";
//    }

    @PostMapping("/checkout/process")
    public String processCheckout(@AuthenticationPrincipal User currentUser,
                                  @RequestParam(required = false) String promocode,
                                  Model model) {
        CartDto cartDto = cartService.getOrCreateCartDto(currentUser);
        if (cartDto.getCartItems() == null || cartDto.getCartItems().isEmpty()) {
            throw new IllegalArgumentException("Корзина пуста");
        }

        List<Long> courseIds = cartDto.getCartItems().stream()
                .map(CartItemDto::getCourseId)
                .collect(Collectors.toList());

        BigDecimal total = cartService.getTotalPriceDto(currentUser);
        BigDecimal discountedTotal = total;
        Promocode promoEntity = null;

        // Применяем промокод, если он передан
        if (promocode != null && !promocode.isEmpty()) {
            try {
                // Используем applyPromocode – он сам проверит валидность и увеличит счётчик
                discountedTotal = promocodeService.applyPromocode(total, promocode);
                promoEntity = promocodeService.findByCode(promocode);
            } catch (IllegalArgumentException e) {
                // Если промокод недействителен – игнорируем
                discountedTotal = total;
                promoEntity = null;
            }
        }

        // Создаём заказ с учётом промокода и скидки
        OrderDto orderDto = orderService.createOrderFromCourseIdsAndReturnDto(
                currentUser.getId(), courseIds, promoEntity, discountedTotal);

        cartService.clearCart(currentUser);

        String paymentUrl = "http://localhost:8080/orders/" + orderDto.getId() + "/pay";
        String qrCodeBase64 = QrCodeGenerator.generateBase64(paymentUrl, 300, 300);

        model.addAttribute("order", orderDto);
        model.addAttribute("qrCode", qrCodeBase64);
        model.addAttribute("showPaymentModal", true);
        model.addAttribute("isAdmin", false);
        model.addAttribute("availableStatuses", OrderStatus.values());
        model.addAttribute("content", "pages/orders/detail :: order-detail-content");
        return "layouts/main";
    }


    @GetMapping("/create")
    public String redirectCreateToCheckout() {
        return "redirect:/orders/checkout";  // ← правильный путь
    }

    @PostMapping("/{id}/confirm-payment")
    public String confirmPayment(@PathVariable Long id,
                                 @AuthenticationPrincipal User user) {
//        Order order = orderService.getOrderById(id);
        Order order = orderService.getOrderByIdWithItems(id);
        if (order.getOrderStatus() == OrderStatus.PENDING) {
            // Обновляем статус на PAID
            orderService.updateOrderStatus(id, OrderStatus.PAID);
            // Выдаём доступ к курсам и сертификаты
            for (OrderItem item : order.getOrderItems()) {
                courseAccessService.grantAccessToCourse(user, item.getCourse(), order);
                // certificateService.generateCertificateForPurchase(user, item.getCourse());
            }
        }
        return "redirect:/orders/" + id;
    }

    @GetMapping("/checkout")
    public String showCheckoutPage(@AuthenticationPrincipal User user, Model model) {
        CartDto cartDto = cartService.getOrCreateCartDto(user);
        BigDecimal total = cartService.getTotalPriceDto(user);

        model.addAttribute("cartItems", cartDto.getCartItems());
        model.addAttribute("totalPrice", total);
        model.addAttribute("title", "Оформление заказа");
        model.addAttribute("content", "pages/orders/checkout :: checkout-content");
        return "layouts/main";
    }

    @PostMapping("/clear-mine")
    public String clearMyOrders(@AuthenticationPrincipal User currentUser) {
        orderService.deleteOrdersByUser(currentUser);
        return "redirect:/orders";
    }

    @PostMapping("/hide-mine")
    public String hideMyOrders(@AuthenticationPrincipal User currentUser) {
        orderService.hideAllOrdersByUser(currentUser);
        return "redirect:/orders";
    }

    @PostMapping("/unhide-mine")
    public String unhideMyOrders(@AuthenticationPrincipal User currentUser) {
        orderService.unhideAllOrdersByUser(currentUser);
        return "redirect:/orders";
    }

    @PostMapping("/apply-promocode")
    public String applyPromocode(@RequestParam String promocode,
                                 @AuthenticationPrincipal User user,
                                 Model model) {
        try {
            BigDecimal currentTotal = cartService.getTotalPriceDto(user);
            Promocode promo = promocodeService.findByCode(promocode);
            if (promo == null) {
                model.addAttribute("promocodeError", "Промокод не найден");
            } else {
                // Используем метод ТОЛЬКО для расчёта (без инкремента)
                BigDecimal discountedTotal = promocodeService.calculateDiscount(currentTotal, promo);
                if (discountedTotal.compareTo(currentTotal) == 0) {
                    model.addAttribute("promocodeError", "Промокод недействителен для данной суммы");
                } else {
                    model.addAttribute("discountedTotal", discountedTotal);
                    model.addAttribute("promocodeApplied", true);
                    model.addAttribute("appliedPromocode", promocode);
                }
            }
        } catch (IllegalArgumentException e) {
            model.addAttribute("promocodeError", e.getMessage());
        }
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