//package com.example.diplomproject.controller;
//
//import com.example.diplomproject.entity.User;
//import com.example.diplomproject.service.BonusService;
//import com.example.diplomproject.service.UserService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.security.access.prepost.PreAuthorize;
//import org.springframework.stereotype.Controller;
//import org.springframework.ui.Model;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.web.servlet.mvc.support.RedirectAttributes;
//
//@Slf4j
//@Controller
//@RequestMapping("/admin/bonuses")
//@PreAuthorize("hasRole('ADMIN')")
//@RequiredArgsConstructor
//public class AdminBonusController {
//
//    private final BonusService bonusService;
//    private final UserService userService;
//
//    /**
//     * Список всех пользователей с их бонусами (можно добавить поиск).
//     */
//    @GetMapping
//    public String listUsersBonuses(Model model) {
//        model.addAttribute("users", userService.getAllUsers()); // нужен метод в UserService
//        model.addAttribute("title", "Управление бонусами");
//        model.addAttribute("content", "pages/admin/bonuses/admin-list :: admin-bonuses-content");
//        return "layouts/main";
//    }
//
//    /**
//     * Форма для начисления бонусов конкретному пользователю.
//     */
//    @GetMapping("/add/{userId}")
//    public String showAddBonusesForm(@PathVariable Long userId, Model model) {
//        User user = userService.getUserById(userId);
//        model.addAttribute("user", user);
//        model.addAttribute("title", "Начисление бонусов");
//        model.addAttribute("content", "pages/admin/bonuses/add-form :: add-bonuses-form");
//        return "layouts/main";
//    }
//
//    /**
//     * Обработка начисления бонусов.
//     */
//    @PostMapping("/add/{userId}")
//    public String addBonuses(@PathVariable Long userId,
//                             @RequestParam int points,
//                             RedirectAttributes redirectAttributes) {
//        try {
//            bonusService.addBonusPoints(userId, points); // нужно реализовать в сервисе
//            redirectAttributes.addAttribute("success", "Начислено " + points + " бонусов пользователю");
//        } catch (Exception e) {
//            log.error("Ошибка начисления бонусов пользователю {}", userId, e);
//            redirectAttributes.addAttribute("error", "Ошибка: " + e.getMessage());
//        }
//        return "redirect:/admin/bonuses";
//    }
//
//    /**
//     * Форма для корректировки бонусов (списание, изменение).
//     */
//    @GetMapping("/adjust/{userId}")
//    public String showAdjustBonusesForm(@PathVariable Long userId, Model model) {
//        User user = userService.getUserById(userId);
//        model.addAttribute("user", user);
//        model.addAttribute("title", "Корректировка бонусов");
//        model.addAttribute("content", "pages/admin/bonuses/adjust-form :: adjust-bonuses-form");
//        return "layouts/main";
//    }
//
//    /**
//     * Обработка корректировки (может быть положительное или отрицательное значение).
//     */
//    @PostMapping("/adjust/{userId}")
//    public String adjustBonuses(@PathVariable Long userId,
//                                @RequestParam int points,
//                                RedirectAttributes redirectAttributes) {
//        try {
//            bonusService.adjustBonusPoints(userId, points); // нужно реализовать
//            String action = points >= 0 ? "Начислено" : "Списано";
//            redirectAttributes.addAttribute("success", action + " " + Math.abs(points) + " бонусов");
//        } catch (Exception e) {
//            log.error("Ошибка корректировки бонусов пользователю {}", userId, e);
//            redirectAttributes.addAttribute("error", "Ошибка: " + e.getMessage());
//        }
//        return "redirect:/admin/bonuses";
//    }
//}