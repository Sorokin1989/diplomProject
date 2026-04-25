package com.example.diplomproject.controller;

import com.example.diplomproject.dto.CartDto;
import com.example.diplomproject.dto.CartItemDto;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
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

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Autowired
    public OrderController(CartService cartService,
                           PromocodeService promocodeService,
                           OrderService orderService,
                           CourseService courseService,
                           CourseAccessService courseAccessService,
                           OrderRepository orderRepository,
                           OrderMapper orderMapper) {
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
        if (currentUser == null) {
            return "redirect:/login";
        }
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
        if (currentUser == null) {
            return "redirect:/login";
        }
        OrderDto orderDto = orderService.getOrderDtoById(id);
        if (orderDto == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Заказ не найден");
        }
        if (!isAdmin(currentUser) && !orderDto.getUserId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Нет доступа к этому заказу");
        }
        // Если заказ в статусе PENDING и это не админ, генерируем QR-код для оплаты
        if (!isAdmin(currentUser) && "PENDING".equals(orderDto.getOrderStatus())) {
            String paymentUrl = baseUrl + "/orders/" + id + "/pay";
            String qrCodeBase64 = QrCodeGenerator.generateBase64(paymentUrl, 300, 300);
            model.addAttribute("qrCode", qrCodeBase64);
        }
        model.addAttribute("isAdmin", isAdmin(currentUser));
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
        if (currentUser == null || !isAdmin(currentUser)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Доступ запрещён");
        }
        orderService.updateOrderStatus(id, status);
        return "redirect:/orders/" + id;
    }

    @PostMapping("/{id}/cancel")
    public String cancelOrder(@PathVariable Long id,
                              @AuthenticationPrincipal User currentUser) {
        if (currentUser == null) {
            return "redirect:/login";
        }
        OrderDto orderDto = orderService.getOrderDtoById(id);
        if (orderDto == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Заказ не найден");
        }
        if (!orderDto.getUserId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Нет доступа");
        }
        if ("PENDING".equals(orderDto.getOrderStatus())) {
            orderService.updateOrderStatus(id, OrderStatus.CANCELLED);
        }
        return "redirect:/orders/" + id;
    }

    @PostMapping("/checkout/process")
    public String processCheckout(@AuthenticationPrincipal User currentUser,
                                  @RequestParam(required = false) String promocode,
                                  RedirectAttributes redirectAttributes,
                                  Model model) {
        if (currentUser == null) {
            return "redirect:/login";
        }
        CartDto cartDto = cartService.getOrCreateCartDto(currentUser);


        if (cartDto.getCartItems() == null || cartDto.getCartItems().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Корзина пуста");
            return "redirect:/cart";
        }

        List<Long> courseIds = cartDto.getCartItems().stream()
                .map(CartItemDto::getCourseId)
                .collect(Collectors.toList());

        BigDecimal total = cartService.getTotalPriceDto(currentUser);
        BigDecimal discountedTotal = total;
        Promocode promoEntity = null;

        if (promocode != null && !promocode.isEmpty()) {
            try {
                // Применяем промокод – он сам проверит валидность и увеличит счётчик
                discountedTotal = promocodeService.applyPromocode(total, promocode);
                promoEntity = promocodeService.findByCode(promocode);
            } catch (IllegalArgumentException e) {
                discountedTotal = total;
                promoEntity = null;
                log.warn("Недействительный промокод при оформлении заказа: {}", promocode);
            }
        }

        // Создаём заказ с учётом промокода и скидки
        OrderDto orderDto = orderService.createOrderFromCourseIdsAndReturnDto(
                currentUser.getId(), courseIds, promoEntity, discountedTotal);

        cartService.clearCart(currentUser);

        String paymentUrl = baseUrl + "/orders/" + orderDto.getId() + "/pay";
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
        return "redirect:/orders/checkout";
    }

    @PostMapping("/{id}/confirm-payment")
    public String confirmPayment(@PathVariable Long id,
                                 @AuthenticationPrincipal User user) {
        if (user == null) {
            return "redirect:/login";
        }
        Order order = orderService.getOrderByIdWithItems(id);
        if (order == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Заказ не найден");
        }
        // Проверка прав: только владелец или администратор
        if (!order.getUser().getId().equals(user.getId()) && !isAdmin(user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Нет доступа");
        }
        if (order.getOrderStatus() == OrderStatus.PENDING) {
            orderService.updateOrderStatus(id, OrderStatus.PAID);
            for (OrderItem item : order.getOrderItems()) {
                courseAccessService.grantAccessToCourse(user, item.getCourse(), order);
            }
        }
        return "redirect:/orders/" + id;
    }

    @GetMapping("/checkout")
    public String showCheckoutPage(@AuthenticationPrincipal User user, Model model) {
        if (user == null) {
            return "redirect:/login";
        }
        CartDto cartDto = cartService.getOrCreateCartDto(user);
        BigDecimal total = cartService.getTotalPriceDto(user);

        model.addAttribute("cartItems", cartDto.getCartItems());
        model.addAttribute("totalPrice", total);
        model.addAttribute("newTotal", total);
        model.addAttribute("title", "Оформление заказа");
        model.addAttribute("content", "pages/orders/checkout :: checkout-content");
        return "layouts/main";
    }

    @PostMapping("/clear-mine")
    public String clearMyOrders(@AuthenticationPrincipal User currentUser) {
        if (currentUser == null) {
            return "redirect:/login";
        }
        orderService.deleteOrdersByUser(currentUser);
        return "redirect:/orders";
    }

    @PostMapping("/hide-mine")
    public String hideMyOrders(@AuthenticationPrincipal User currentUser) {
        if (currentUser == null) {
            return "redirect:/login";
        }
        orderService.hideAllOrdersByUser(currentUser);
        return "redirect:/orders";
    }

    @PostMapping("/unhide-mine")
    public String unhideMyOrders(@AuthenticationPrincipal User currentUser) {
        if (currentUser == null) {
            return "redirect:/login";
        }
        orderService.unhideAllOrdersByUser(currentUser);
        return "redirect:/orders";
    }

    @PostMapping("/apply-promocode")
    public String applyPromocode(@RequestParam String promocode,
                                 @AuthenticationPrincipal User user,
                                 Model model) {
        if (user == null) {
            return "redirect:/login";
        }
        try {
            BigDecimal currentTotal = cartService.getTotalPriceDto(user);
            Promocode promo = promocodeService.findByCode(promocode);
            if (promo == null) {
                model.addAttribute("promocodeError", "Промокод не найден");
            } else {
                BigDecimal discountAmount = promocodeService.calculateDiscount(currentTotal, promo);
                BigDecimal newTotal = currentTotal.subtract(discountAmount);
                if (newTotal.compareTo(currentTotal) == 0) {
                    model.addAttribute("promocodeError", "Промокод недействителен для данной суммы");
                } else {
                    model.addAttribute("newTotal", newTotal);
                    model.addAttribute("discountAmount", discountAmount);
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
        return user != null && user.getRole() == Role.ADMIN;
    }
}