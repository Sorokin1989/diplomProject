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
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


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

        if (user == null) {
            return "redirect:/login";
        }
        List<Certificate> certificates = certificateService.getCertificatesByUser(user);
        List<CertificateDto> certificateDtos = certificates.stream()
                .map(certificateMapper::toCertificateDto)
                .toList();

        model.addAttribute("certificates", certificateDtos);
        model.addAttribute("title", "Мои сертификаты");
        model.addAttribute("content", "pages/certificates/list :: certificates-list-content");
        return "layouts/main";
    }

    @GetMapping("/{id}")
    public String viewCertificate(@PathVariable Long id, @AuthenticationPrincipal User currentUser, Model model) {
        Certificate certificate = certificateService.getCertificateById(id);

        // Проверка: текущий пользователь - владелец или администратор
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        if (!certificate.getUser().getId().equals(currentUser.getId()) && !currentUser.isAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Нет доступа к этому сертификату");
        }

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

        // Безопасное построение пути
        String fileName = certificate.getCertificateUrl(); // "123.pdf"
        Path basePath = Paths.get("uploads/certificates").toAbsolutePath().normalize();
        Path filePath = basePath.resolve(fileName).normalize();

        if (!filePath.startsWith(basePath)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Некорректный путь к файлу");
        }

        try {
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
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