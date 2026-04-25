package com.example.diplomproject.controller;

import com.example.diplomproject.entity.Certificate;
import com.example.diplomproject.entity.Course;
import com.example.diplomproject.entity.User;
import com.example.diplomproject.enums.Role;
import com.example.diplomproject.service.CertificateService;
import com.example.diplomproject.service.CourseService;
import com.example.diplomproject.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequestMapping("/admin/certificates")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminCertificateController {

    @Value("${app.upload.path}")
    private String uploadPath;

    private final CertificateService certificateService;
    private final UserService userService;
    private final CourseService courseService;

    @GetMapping
    public String listCertificates(Model model) {
        model.addAttribute("certificates", certificateService.getAllCertificates());
        model.addAttribute("title", "Управление сертификатами");
        model.addAttribute("content", "pages/admin/certificates/admin-list :: admin-certificates-content");
        return "layouts/main";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("users", userService.getAllUsers().stream()
                .filter(user -> user.getRole() != Role.ADMIN)
                .collect(Collectors.toList()));
        // Для админки используем метод, возвращающий сущности
        model.addAttribute("courses", courseService.getAllCoursesForAdmin());
        model.addAttribute("title", "Создание сертификата");
        model.addAttribute("content", "pages/admin/certificates/form :: admin-certificate-form");
        return "layouts/main";
    }

    @PostMapping("/new")
    public String createCertificate(@RequestParam("userId") Long userId,
                                    @RequestParam("courseId") Long courseId,
                                    @RequestParam("certificateFile") MultipartFile file,
                                    RedirectAttributes redirectAttributes) {
        if (file == null || file.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Файл сертификата не выбран");
            return "redirect:/admin/certificates/new";
        }
        User user = userService.getUserById(userId);
        if (user == null) {
            redirectAttributes.addFlashAttribute("error", "Пользователь не найден");
            return "redirect:/admin/certificates/new";
        }
        Course course = courseService.getCourseEntityById(courseId);
        if (course == null) {
            redirectAttributes.addFlashAttribute("error", "Курс не найден");
            return "redirect:/admin/certificates/new";
        }
        try {
            certificateService.createManualCertificateWithFile(user, course, file);
            redirectAttributes.addFlashAttribute("success", "Сертификат успешно создан и загружен");
        } catch (Exception e) {
            log.error("Ошибка создания сертификата", e);
            redirectAttributes.addFlashAttribute("error", "Ошибка: " + e.getMessage());
        }
        return "redirect:/admin/certificates";
    }
    @GetMapping("/{id}")
    public String viewCertificate(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Certificate certificate = certificateService.getCertificateById(id);
        if (certificate == null) {
            redirectAttributes.addFlashAttribute("error", "Сертификат не найден");
            return "redirect:/admin/certificates";
        }
        model.addAttribute("certificate", certificate);
        model.addAttribute("title", "Просмотр сертификата");
        model.addAttribute("content", "pages/admin/certificates/view :: admin-certificate-view");
        return "layouts/main";
    }

    @PostMapping("/{id}/revoke")
    public String revokeCertificate(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Certificate certificate = certificateService.getCertificateById(id);
        if (certificate == null) {
            redirectAttributes.addFlashAttribute("error", "Сертификат не найден");
            return "redirect:/admin/certificates";
        }
        try {
            certificateService.revokeCertificate(id);
            redirectAttributes.addFlashAttribute("success", "Сертификат отозван");
        } catch (Exception e) {
            log.error("Ошибка отзыва сертификата {}", id, e);
            redirectAttributes.addFlashAttribute("error", "Ошибка отзыва: " + e.getMessage());
        }
        return "redirect:/admin/certificates";
    }
    @PostMapping("/{id}/activate")
    public String activateCertificate(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            certificateService.activateCertificate(id);
            redirectAttributes.addFlashAttribute("success", "Сертификат активирован");
        } catch (Exception e) {
            log.error("Ошибка активации сертификата {}", id, e);
            redirectAttributes.addFlashAttribute("error", "Ошибка активации: " + e.getMessage());
        }
        return "redirect:/admin/certificates";
    }

    @PostMapping("/{id}/delete")
    public String deleteCertificate(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            certificateService.deleteCertificate(id);
            redirectAttributes.addFlashAttribute("success", "Сертификат удалён");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка удаления: " + e.getMessage());
        }
        return "redirect:/admin/certificates";
    }
}