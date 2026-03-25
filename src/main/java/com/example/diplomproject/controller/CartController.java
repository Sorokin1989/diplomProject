package com.example.diplomproject.controller;

import com.example.diplomproject.entity.Course;
import com.example.diplomproject.entity.User;
import com.example.diplomproject.service.CartService;
import com.example.diplomproject.service.CourseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

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
        model.addAttribute("cartItems", cartService.getAllItems(user));
        model.addAttribute("totalPrice", cartService.getTotalPrice(user));
        return "cart/view";
    }

    @PostMapping("/add/{courseId}")
    public String addToCart(@AuthenticationPrincipal User user,
                            @PathVariable Long courseId,
                            @RequestParam(defaultValue = "1") int quantity) {
        Course course = courseService.getCourseById(courseId); // предполагаем, что такой метод есть
        cartService.addCourseToCart(user, course, quantity);
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