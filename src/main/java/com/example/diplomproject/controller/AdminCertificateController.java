package com.example.diplomproject.controller;

import com.example.diplomproject.entity.Certificate;
import com.example.diplomproject.service.CertificateService;
import com.example.diplomproject.service.CourseService;
import com.example.diplomproject.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequestMapping("/admin/certificates")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminCertificateController {

    private final CertificateService certificateService;
    private final UserService userService;
    private final CourseService courseService;

    // Список всех сертификатов
    @GetMapping
    public String listCertificates(Model model) {
        model.addAttribute("certificates", certificateService.getAllCertificates());
        model.addAttribute("title", "Управление сертификатами");
        model.addAttribute("content", "pages/admin/certificates/admin-list :: admin-certificates-content");
        return "layouts/main";
    }

    // Форма создания сертификата
    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("users", userService.getAllUsers());
        model.addAttribute("courses", courseService.getAllCourses());
        model.addAttribute("title", "Создание сертификата");
        model.addAttribute("content", "pages/admin/certificates/form :: admin-certificate-form");
        return "layouts/main";
    }

    // Обработка создания с загрузкой файла
    @PostMapping("/new")
    public String createCertificate(@RequestParam("userId") Long userId,
                                    @RequestParam("courseId") Long courseId,
                                    @RequestParam("certificateFile") MultipartFile file,
                                    RedirectAttributes redirectAttributes) {
        try {
            certificateService.createManualCertificateWithFile(
                    userService.getUserById(userId),
                    courseService.getCourseById(courseId),
                    file
            );
            redirectAttributes.addAttribute("success", "Сертификат успешно создан и загружен");
        } catch (Exception e) {
            log.error("Ошибка создания сертификата", e);
            redirectAttributes.addAttribute("error", "Ошибка: " + e.getMessage());
        }
        return "redirect:/admin/certificates";
    }

    // Детальный просмотр сертификата
    @GetMapping("/{id}")
    public String viewCertificate(@PathVariable Long id, Model model) {
        Certificate certificate = certificateService.getCertificateById(id);
        model.addAttribute("certificate", certificate);
        model.addAttribute("title", "Просмотр сертификата");
        model.addAttribute("content", "pages/admin/certificates/view :: admin-certificate-view");
        return "layouts/main";
    }

    // Отзыв сертификата
    @PostMapping("/{id}/revoke")
    public String revokeCertificate(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            certificateService.revokeCertificate(id);
            redirectAttributes.addAttribute("success", "Сертификат отозван");
        } catch (Exception e) {
            log.error("Ошибка отзыва сертификата {}", id, e);
            redirectAttributes.addAttribute("error", "Ошибка отзыва: " + e.getMessage());
        }
        return "redirect:/admin/certificates";
    }
}