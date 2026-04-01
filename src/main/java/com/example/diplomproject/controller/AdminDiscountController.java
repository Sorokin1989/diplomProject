package com.example.diplomproject.controller;

import com.example.diplomproject.entity.Discount;
import com.example.diplomproject.enums.DiscountType;
import com.example.diplomproject.service.DiscountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequestMapping("/admin/discounts")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminDiscountController {

    private final DiscountService discountService;

    @GetMapping
    public String listDiscounts(Model model) {
        model.addAttribute("discounts", discountService.getAllDiscounts());
        model.addAttribute("title", "Управление скидками");
        model.addAttribute("content", "pages/admin/discounts/admin-list :: admin-discounts-content");
        return "layouts/main";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("discount", new Discount());
        model.addAttribute("discountTypes", DiscountType.values());  // добавлено
        model.addAttribute("title", "Создание скидки");
        model.addAttribute("content", "pages/admin/discounts/form :: discount-form");
        return "layouts/main";
    }

    @PostMapping
    public String createDiscount(@ModelAttribute Discount discount,
                                 RedirectAttributes redirectAttributes) {
        try {
            discountService.createNewDiscount(discount);
            redirectAttributes.addAttribute("success", "Скидка успешно создана");
            return "redirect:/admin/discounts";
        } catch (Exception e) {
            log.error("Ошибка создания скидки", e);
            redirectAttributes.addAttribute("error", "Ошибка создания: " + e.getMessage());
            return "redirect:/admin/discounts/new";
        }
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Discount discount = discountService.getDiscountById(id);
        model.addAttribute("discount", discount);
        model.addAttribute("discountTypes", DiscountType.values());  // добавлено
        model.addAttribute("title", "Редактирование скидки");
        model.addAttribute("content", "pages/admin/discounts/form :: discount-form");
        return "layouts/main";
    }

    @PostMapping("/{id}")
    public String updateDiscount(@PathVariable Long id,
                                 @ModelAttribute Discount discount,
                                 RedirectAttributes redirectAttributes) {
        try {
            discountService.updateDiscount(id, discount);
            redirectAttributes.addAttribute("success", "Скидка обновлена");
            return "redirect:/admin/discounts";
        } catch (Exception e) {
            log.error("Ошибка обновления скидки {}", id, e);
            redirectAttributes.addAttribute("error", "Ошибка обновления: " + e.getMessage());
            return "redirect:/admin/discounts/edit/" + id;
        }
    }

    @PostMapping("/delete/{id}")
    public String deleteDiscount(@PathVariable Long id,
                                 RedirectAttributes redirectAttributes) {
        try {
            discountService.deleteDiscount(id);
            redirectAttributes.addAttribute("success", "Скидка удалена");
        } catch (Exception e) {
            log.error("Ошибка удаления скидки {}", id, e);
            redirectAttributes.addAttribute("error", "Ошибка удаления: " + e.getMessage());
        }
        return "redirect:/admin/discounts";
    }
}