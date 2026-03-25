package com.example.diplomproject.controller;

import com.example.diplomproject.entity.Certificate;
import com.example.diplomproject.entity.User;
import com.example.diplomproject.service.CertificateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/certificates")
public class CertificateController {

    private final CertificateService certificateService;

    @Autowired
    public CertificateController(CertificateService certificateService) {
        this.certificateService = certificateService;
    }

    /**
     * Отображает список сертификатов текущего пользователя.
     */
    @GetMapping
    public String listUserCertificates(@AuthenticationPrincipal User user, Model model) {
        List<Certificate> certificates = certificateService.getCertificatesByUser(user);
        model.addAttribute("certificates", certificates);
        return "pages/certificates/list";   // шаблон /templates/pages/certificates/list.html
    }

    /**
     * Отображает отдельный сертификат по его ID (детальная страница).
     */
    @GetMapping("/{id}")
    public String viewCertificate(@PathVariable Long id, Model model) {
        Certificate certificate = certificateService.getCertificateById(id);
        model.addAttribute("certificate", certificate);
        return "pages/certificates/view";   // шаблон /templates/pages/certificates/view.html
    }
}