package com.example.diplomproject.controller;

import com.example.diplomproject.dto.CartDto;
import com.example.diplomproject.dto.CartItemDto;
import com.example.diplomproject.dto.CourseDto;
import com.example.diplomproject.entity.User;
import com.example.diplomproject.service.CartService;
import com.example.diplomproject.service.CourseService;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpServletRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

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
        if (user == null) {
            return "redirect:/login";
        }
        CartDto cartDto = cartService.getOrCreateCartDto(user);
        List<CartItemDto> items = cartDto.getCartItems() != null ? cartDto.getCartItems() : List.of();
        BigDecimal total = cartService.getTotalPriceDto(user);

        model.addAttribute("title", "Корзина");
        model.addAttribute("content", "pages/cart/cart :: cart-content");
        model.addAttribute("cartItems", items);
        model.addAttribute("totalPrice", total);
        return "layouts/main";
    }



    @PostMapping("/add/{id}")
    public String addToCart(@AuthenticationPrincipal User user,
                            @PathVariable Long id,
                            RedirectAttributes redirectAttributes,
                            HttpServletRequest request) {

        if (user == null) {
            return "redirect:/login";
        }
        try {
            CourseDto dto = courseService.getCourseDtoById(id);
            cartService.addCourseToCart(user, dto);
            redirectAttributes.addFlashAttribute("successMessage", "Курс добавлен в корзину");
        } catch (EntityNotFoundException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Курс не найден");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error adding to cart: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при добавлении курса");
        }
        String referer = request.getHeader("Referer");
        if (referer != null && (referer.contains("/courses") || referer.contains("/categories"))) {
            return "redirect:" + referer;
        }
        return "redirect:/courses";
    }

    @PostMapping("/remove/{cartItemId}")
    public String removeCartItem(@AuthenticationPrincipal User user,
                                 @PathVariable Long cartItemId,
                                 RedirectAttributes redirectAttributes) {
        if (user == null) return "redirect:/login";
        try {
            cartService.removeCourseFromCart(user, cartItemId);
            redirectAttributes.addFlashAttribute("successMessage", "Товар удалён из корзины");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            log.error("Error removing cart item: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при удалении товара");
        }
        return "redirect:/cart";
    }

    @PostMapping("/clear")
    public String clearCart(@AuthenticationPrincipal User user,
                            RedirectAttributes redirectAttributes) {
        if (user == null) return "redirect:/login";
        try {
            cartService.clearCart(user);
            redirectAttributes.addFlashAttribute("successMessage", "Корзина очищена");
        } catch (Exception e) {
            log.error("Error clearing cart: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при очистке корзины");
        }
        return "redirect:/cart";
    }

    @GetMapping("/count")
    @ResponseBody
    public Map<String, Integer> getCartCount(@AuthenticationPrincipal User user) {
        if (user == null) {
            return Map.of("count", 0);
        }
        int count = cartService.getCartItemCount(user);
        return Map.of("count", count);
    }
}