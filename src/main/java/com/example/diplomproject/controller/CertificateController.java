package com.example.diplomproject.controller;

import com.example.diplomproject.dto.CertificateDto;
import com.example.diplomproject.entity.Certificate;
import com.example.diplomproject.entity.User;
import com.example.diplomproject.mapper.CertificateMapper;
import com.example.diplomproject.service.CertificateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/certificates")
public class CertificateController {

    private final CertificateService certificateService;
    private final CertificateMapper certificateMapper;

    @Autowired
    public CertificateController(CertificateService certificateService, CertificateMapper certificateMapper) {
        this.certificateService = certificateService;
        this.certificateMapper = certificateMapper;
    }

    @GetMapping
    public String listUserCertificates(@AuthenticationPrincipal User user, Model model) {
        List<Certificate> certificates = certificateService.getCertificatesByUser(user);
        List<CertificateDto> certificateDtos = certificates.stream()
                .map(certificateMapper::toCertificateDto)
                .collect(Collectors.toList());

        model.addAttribute("certificates", certificateDtos);
        model.addAttribute("title", "Мои сертификаты");
        model.addAttribute("content", "pages/certificates/list :: certificates-list-content");
        return "layouts/main";
    }

    @GetMapping("/{id}")
    public String viewCertificate(@PathVariable Long id, Model model) {
        Certificate certificate = certificateService.getCertificateById(id);
        CertificateDto certificateDto = certificateMapper.toCertificateDto(certificate);

        model.addAttribute("certificate", certificateDto);
        model.addAttribute("title", "Сертификат");
        model.addAttribute("content", "pages/certificates/view :: certificate-view-content");
        return "layouts/main";
    }
}