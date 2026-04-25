package com.example.diplomproject.controller;

import com.example.diplomproject.dto.CertificateDto;
import com.example.diplomproject.entity.Certificate;
import com.example.diplomproject.entity.User;
import com.example.diplomproject.mapper.CertificateMapper;
import com.example.diplomproject.service.CertificateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;


@Controller
@RequestMapping("/certificates")
public class CertificateController {

    @Value("${app.upload.path}")
    private String uploadPath;

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

        // Получаем имя файла (без пути)
        String fileName = Paths.get(certificate.getCertificateUrl()).getFileName().toString();
        Path basePath = Paths.get(uploadPath).toAbsolutePath().normalize();
        Path filePath = basePath.resolve("certificates").resolve(fileName).normalize();

        if (!filePath.startsWith(basePath)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Некорректный путь к файлу");
        }

        // Проверяем, что файл существует и читается
        if (!Files.isRegularFile(filePath) || !Files.isReadable(filePath)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Файл сертификата не найден или не читается");
        }

        // Используем FileSystemResource вместо UrlResource
        Resource resource = new FileSystemResource(filePath.toFile());
        // Определяем content-type (можно упростить)
        String lowerFileName = filePath.getFileName().toString().toLowerCase();
        String contentType;
        if (lowerFileName.endsWith(".png")) {
            contentType = "image/png";
        } else if (lowerFileName.endsWith(".jpg") || lowerFileName.endsWith(".jpeg")) {
            contentType = "image/jpeg";
        } else if (lowerFileName.endsWith(".pdf")) {
            contentType = "application/pdf";
        } else {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(resource);
    }

    @GetMapping("/download-file/{id}")
    public ResponseEntity<Resource> downloadCertificateFile(@PathVariable Long id,
                                                        @AuthenticationPrincipal User user) {
        Certificate certificate = certificateService.getCertificateById(id);
        if (certificate == null || certificate.isRevoked() || certificate.getCertificateUrl() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Сертификат не найден или отозван");
        }
        if (!certificate.getUser().getId().equals(user.getId()) && !user.isAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Нет доступа");
        }

        String fileName = Paths.get(certificate.getCertificateUrl()).getFileName().toString();
        Path basePath = Paths.get(uploadPath).toAbsolutePath().normalize();
        Path filePath = basePath.resolve("certificates").resolve(fileName).normalize();

        if (!filePath.startsWith(basePath)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Некорректный путь");
        }
        if (!Files.isRegularFile(filePath) || !Files.isReadable(filePath)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Файл сертификата не найден");
        }

        Resource resource = new FileSystemResource(filePath.toFile());
        String contentType = determineContentTypeByExtension(fileName);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(resource);
    }

    private String determineContentTypeByExtension(String fileName) {
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".pdf")) return "application/pdf";
        return "application/octet-stream";
    }
}