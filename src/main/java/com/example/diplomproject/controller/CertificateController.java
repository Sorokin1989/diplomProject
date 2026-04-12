package com.example.diplomproject.controller;

import com.example.diplomproject.dto.CertificateDto;
import com.example.diplomproject.entity.Certificate;
import com.example.diplomproject.entity.User;
import com.example.diplomproject.mapper.CertificateMapper;
import com.example.diplomproject.service.CertificateService;
import com.example.diplomproject.service.CourseAccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/certificates")
public class CertificateController {

    private final CertificateService certificateService;
    private final CertificateMapper certificateMapper;
    private final CourseAccessService courseAccessService;

    @Autowired
    public CertificateController(CertificateService certificateService, CertificateMapper certificateMapper, CourseAccessService courseAccessService) {
        this.certificateService = certificateService;
        this.certificateMapper = certificateMapper;
        this.courseAccessService = courseAccessService;
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

    @GetMapping("/download/{courseId}")
    public ResponseEntity<Resource> downloadCertificate(@PathVariable Long courseId,
                                                        @AuthenticationPrincipal User user) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        Certificate certificate = certificateService.findByUserAndCourse(user.getId(), courseId);
        if (certificate == null || certificate.isRevoked() || certificate.getCertificateUrl() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Сертификат не найден или отозван");
        }
        if (!courseAccessService.hasAccessToUser(user, certificate.getCourse())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Доступ запрещён");
        }
        try {
            // Убираем ведущий слеш, если есть
            String relativePath = certificate.getCertificateUrl().startsWith("/")
                    ? certificate.getCertificateUrl().substring(1)
                    : certificate.getCertificateUrl();
            Path filePath = Paths.get(relativePath).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                // Определяем MIME-тип по расширению файла
                String contentType = determineContentType(filePath);
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filePath.getFileName().toString() + "\"")
                        .body(resource);
            } else {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Файл сертификата не найден");
            }
        } catch (MalformedURLException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Ошибка чтения файла");
        }
    }

    private String determineContentType(Path filePath) {
        String fileName = filePath.getFileName().toString().toLowerCase();
        if (fileName.endsWith(".pdf")) {
            return "application/pdf";
        } else if (fileName.endsWith(".png")) {
            return "image/png";
        } else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (fileName.endsWith(".gif")) {
            return "image/gif";
        } else {
            // По умолчанию - бинарный поток
            return "application/octet-stream";
        }
    }
}