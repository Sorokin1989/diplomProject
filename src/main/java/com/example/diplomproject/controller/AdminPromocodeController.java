package com.example.diplomproject.controller.admin;

import com.example.diplomproject.entity.Promocode;
import com.example.diplomproject.enums.DiscountType;
import com.example.diplomproject.service.PromocodeService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/admin/promocodes")
@PreAuthorize("hasRole('ADMIN')")
public class AdminPromocodeController {

    private final PromocodeService promocodeService;

    public AdminPromocodeController(PromocodeService promocodeService) {
        this.promocodeService = promocodeService;
    }

    @GetMapping
    public String listPromocodes(Model model) {
        List<Promocode> promocodes = promocodeService.getAllPromocodes(); // нужно добавить метод в сервис
        model.addAttribute("promocodes", promocodes);
        return "admin/promocodes/list";
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("promocode", new Promocode());
        model.addAttribute("discountTypes", DiscountType.values());
        return "admin/promocodes/form";
    }

    @PostMapping
    public String createPromocode(@RequestParam String code,
                                  @RequestParam DiscountType discountType,
                                  @RequestParam BigDecimal value,
                                  @RequestParam BigDecimal minOrderAmount,
                                  @RequestParam(required = false) Integer usageLimit,
                                  @RequestParam(required = false) LocalDateTime validFrom,
                                  @RequestParam(required = false) LocalDateTime validTo) {
        promocodeService.createPromoCode(code, discountType, value, minOrderAmount, usageLimit, validFrom, validTo);
        return "redirect:/admin/promocodes";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Promocode promocode = promocodeService.getPromocodeById(id); // нужен метод в сервисе
        model.addAttribute("promocode", promocode);
        model.addAttribute("discountTypes", DiscountType.values());
        return "admin/promocodes/form";
    }

    @PostMapping("/{id}")
    public String updatePromocode(@PathVariable Long id,
                                  @RequestParam String code,
                                  @RequestParam DiscountType discountType,
                                  @RequestParam BigDecimal value,
                                  @RequestParam BigDecimal minOrderAmount,
                                  @RequestParam(required = false) Integer usageLimit,
                                  @RequestParam(required = false) LocalDateTime validFrom,
                                  @RequestParam(required = false) LocalDateTime validTo,
                                  @RequestParam boolean active) {
        // обновление промокода (нужно добавить метод в сервис)
        promocodeService.updatePromocode(id, code, discountType, value, minOrderAmount, usageLimit, validFrom, validTo, active);
        return "redirect:/admin/promocodes";
    }

    @PostMapping("/delete/{id}")
    public String deletePromocode(@PathVariable Long id) {
        promocodeService.deletePromocode(id);
        return "redirect:/admin/promocodes";
    }
}