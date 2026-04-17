package com.example.diplomproject.controller;

import com.example.diplomproject.entity.Promocode;
import com.example.diplomproject.enums.DiscountType;
import com.example.diplomproject.service.PromocodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.NoSuchElementException;

@Slf4j
@Controller
@RequestMapping("/admin/promocodes")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminPromocodeController {

    private final PromocodeService promocodeService;

    @GetMapping
    public String listPromocodes(Model model) {
        model.addAttribute("promocodes", promocodeService.getAllPromocodes());
        model.addAttribute("title", "Управление промокодами");
        model.addAttribute("content", "pages/admin/promocodes/admin-list :: admin-promocodes-content");
        return "layouts/main";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("promocode", new Promocode());
        model.addAttribute("discountTypes", DiscountType.values());
        model.addAttribute("title", "Создание промокода");
        model.addAttribute("content", "pages/admin/promocodes/form :: promo-form");
        return "layouts/main";
    }

    @PostMapping
    public String createPromocode(@ModelAttribute Promocode promocode,
                                  RedirectAttributes redirectAttributes) {
        try {
            promocodeService.createPromoCode(promocode);
            redirectAttributes.addAttribute("success", "Промокод успешно создан");
            return "redirect:/admin/promocodes";
        } catch (Exception e) {
            log.error("Ошибка создания промокода", e);
            redirectAttributes.addAttribute("error", "Ошибка создания: " + e.getMessage());
            return "redirect:/admin/promocodes/new";
        }
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            Promocode promocode = promocodeService.getPromocodeById(id);
            model.addAttribute("promocode", promocode);
            model.addAttribute("discountTypes", DiscountType.values());
            model.addAttribute("title", "Редактирование промокода");
            model.addAttribute("content", "pages/admin/promocodes/form :: promo-form");
            return "layouts/main";
        } catch (NoSuchElementException e) {
            redirectAttributes.addFlashAttribute("error", "Промокод не найден");
            return "redirect:/admin/promocodes";
        }
    }

    @PostMapping("/{id}")
    public String updatePromocode(@PathVariable Long id,
                                  @ModelAttribute Promocode promocode,
                                  RedirectAttributes redirectAttributes) {
        try {
            promocodeService.updatePromocode(id, promocode);
            redirectAttributes.addFlashAttribute("success", "Промокод обновлён");
            return "redirect:/admin/promocodes";
        } catch (NoSuchElementException e) {
            redirectAttributes.addFlashAttribute("error", "Промокод не найден");
            return "redirect:/admin/promocodes";
        } catch (Exception e) {
            log.error("Ошибка обновления промокода {}", id, e);
            redirectAttributes.addFlashAttribute("error", "Ошибка обновления: " + e.getMessage());
            return "redirect:/admin/promocodes/edit/" + id;
        }
    }

    @PostMapping("/delete/{id}")
    public String deletePromocode(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            promocodeService.deletePromocode(id);
            redirectAttributes.addFlashAttribute("success", "Промокод удалён");
            return "redirect:/admin/promocodes";
        } catch (NoSuchElementException e) {
            redirectAttributes.addFlashAttribute("error", "Промокод не найден");
            return "redirect:/admin/promocodes";
        } catch (Exception e) {
            log.error("Ошибка удаления промокода {}", id, e);
            redirectAttributes.addFlashAttribute("error", "Ошибка удаления: " + e.getMessage());
            return "redirect:/admin/promocodes";
        }
    }
}