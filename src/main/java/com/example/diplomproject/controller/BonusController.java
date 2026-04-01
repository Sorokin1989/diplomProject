//package com.example.diplomproject.controller;
//
//import com.example.diplomproject.entity.User;
//import com.example.diplomproject.service.BonusService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.security.core.annotation.AuthenticationPrincipal;
//import org.springframework.stereotype.Controller;
//import org.springframework.ui.Model;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RequestParam;
//
//@Controller
//@RequestMapping("/bonuses")
//public class BonusController {
//
//    private final BonusService bonusService;
//    @Autowired
//    public BonusController(BonusService bonusService) {
//        this.bonusService = bonusService;
//    }
//
//    /**
//     * Отображает страницу с текущим балансом бонусов пользователя.
//     */
//    @GetMapping
//    public String showBonusesPage(@AuthenticationPrincipal User currentUser, Model model) {
//        int currentPoints = bonusService.getCurrentBonusPoints(currentUser);
//        model.addAttribute("currentPoints", currentPoints);
//        // Здесь можно добавить историю операций с бонусами, если она хранится
//        return "/pages/bonuses/index";
//    }
//
//    /**
//     * Обрабатывает списание бонусов.
//     */
//    @PostMapping("/spend")
//    public String spendBonuses(@AuthenticationPrincipal User currentUser,
//                               @RequestParam("points") int pointsToSpend,
//                               Model model) {
//        boolean success = bonusService.spendBonusPoints(currentUser, pointsToSpend);
//        if (success) {
//            model.addAttribute("message", "Списано " + pointsToSpend + " бонусов.");
//        } else {
//            model.addAttribute("error", "Недостаточно бонусов для списания.");
//        }
//        int currentPoints = bonusService.getCurrentBonusPoints(currentUser);
//        model.addAttribute("currentPoints", currentPoints);
//        return "pages/bonuses/index";
//    }
//}