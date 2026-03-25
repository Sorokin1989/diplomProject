package com.example.diplomproject.controller;

import com.example.diplomproject.entity.Discount;
import com.example.diplomproject.service.DiscountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/admin/discounts")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDiscountController {

    private final DiscountService discountService;
    @Autowired
    public AdminDiscountController(DiscountService discountService) {
        this.discountService = discountService;
    }

    // Список всех скидок
    @GetMapping
    public String listDiscounts(Model model) {
        List<Discount> discounts = discountService.getAllDiscounts();
        model.addAttribute("discounts", discounts);
        return "pages/admin/discounts/list";
    }

    // Форма создания новой скидки
    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("discount", new Discount());
        return "pages/admin/discounts/form";
    }

    // Обработка создания скидки
    @PostMapping
    public String createDiscount(@ModelAttribute("discount") Discount discount) {
        discountService.createNewDiscount(discount);
        return "redirect:/admin/discounts";
    }

    // Форма редактирования скидки
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Discount discount = discountService.getDiscountById(id);
        model.addAttribute("discount", discount);
        return "pages/admin/discounts/form";
    }

    // Обработка обновления скидки
    @PostMapping("/{id}")
    public String updateDiscount(@PathVariable Long id, @ModelAttribute("discount") Discount updatedDiscount) {
        discountService.updateDiscount(id, updatedDiscount);
        return "redirect:/admin/discounts";
    }

    // Удаление скидки (POST-запрос для безопасности)
    @PostMapping("/delete/{id}")
    public String deleteDiscount(@PathVariable Long id) {
        discountService.deleteDiscount(id);
        return "redirect:/admin/discounts";
    }
}