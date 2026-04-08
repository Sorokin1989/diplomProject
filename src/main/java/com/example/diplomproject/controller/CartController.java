package com.example.diplomproject.controller;

import com.example.diplomproject.dto.CartDto;
import com.example.diplomproject.dto.CartItemDto;
import com.example.diplomproject.entity.Course;
import com.example.diplomproject.entity.User;
import com.example.diplomproject.service.CartService;
import com.example.diplomproject.service.CourseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Controller
@RequestMapping("/cart")
public class CartController {

    private final CartService cartService;
    private final CourseService courseService;

    @Autowired
    public CartController(CartService cartService, CourseService courseService) {
        this.cartService = cartService;
        this.courseService = courseService;
    }

    @GetMapping
    public String viewCart(@AuthenticationPrincipal User user, Model model) {
        // Получаем DTO корзины (гарантированно существует)
        CartDto cartDto = cartService.getOrCreateCartDto(user);
        List<CartItemDto> items = cartDto.getCartItems(); // или через getAllItemsDto
        BigDecimal total = cartService.getTotalPriceDto(user);

        model.addAttribute("title", "Корзина");
        model.addAttribute("content", "pages/cart/cart :: cart-content");
        model.addAttribute("cartItems", items);
        model.addAttribute("totalPrice", total);
        return "layouts/main";
    }

    @PostMapping("/add/{courseId}")
    public String addToCart(@AuthenticationPrincipal User user,
                            @PathVariable Long courseId,
                            @RequestParam(defaultValue = "1") int quantity) {
        Course course = courseService.getCourseById(courseId);
        cartService.addCourseToCart(user, course, quantity); // метод работает с сущностями – оставляем
        return "redirect:/cart";
    }

    @PostMapping("/remove/{cartItemId}")
    public String removeCartItem(@AuthenticationPrincipal User user,
                                 @PathVariable Long cartItemId) {
        cartService.removeCourseFromCart(user, cartItemId);
        return "redirect:/cart";
    }

    @PostMapping("/update/{cartItemId}")
    public String updateQuantity(@AuthenticationPrincipal User user,
                                 @PathVariable Long cartItemId,
                                 @RequestParam int quantity) {
        cartService.updateQuantity(user, cartItemId, quantity);
        return "redirect:/cart";
    }

    @PostMapping("/clear")
    public String clearCart(@AuthenticationPrincipal User user) {
        cartService.clearCart(user);
        return "redirect:/cart";
    }
}