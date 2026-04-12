package com.example.diplomproject.service;

import com.example.diplomproject.entity.Certificate;
import com.example.diplomproject.entity.Course;
import com.example.diplomproject.entity.User;
import com.example.diplomproject.repository.CertificateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
public class CertificateService {

    private final CertificateRepository certificateRepository;

    // Автоматическая выдача при покупке
//    @Transactional
//    public void generateCertificateForPurchase(User user, Course course) {
//        if (user == null || course == null) {
//            throw new IllegalArgumentException("Пользователь и курс не могут быть null");
//        }
//        if (certificateRepository.existsByUserAndCourse(user, course)) {
//            throw new IllegalStateException("Сертификат на этот курс уже выдан");
//        }
//
//        Certificate certificate = new Certificate();
//        certificate.setUser(user);
//        certificate.setCourse(course);
//        certificate.setCreatedAt(LocalDateTime.now());
//        certificate.setCertificateId(generateCertificateNumber());
//        certificate.setRevoked(false);
//
//        certificateRepository.save(certificate);
//    }

    // Ручное создание сертификата с загрузкой файла
    @Transactional
    public void createManualCertificateWithFile(User user, Course course, MultipartFile file) {
        if (user == null || course == null) {
            throw new IllegalArgumentException("Пользователь и курс обязательны");
        }
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Файл сертификата не загружен");
        }
        if (certificateRepository.existsByUserAndCourse(user, course)) {
            throw new IllegalStateException("Сертификат на этот курс уже существует");
        }

        // Сначала создаём запись без URL
        Certificate certificate = new Certificate();
        certificate.setUser(user);
        certificate.setCourse(course);
        certificate.setCreatedAt(LocalDateTime.now());
        certificate.setCertificateId(generateCertificateNumber());
        certificate.setRevoked(false);
        // URL пока не устанавливаем

        // Сохраняем запись в БД (чтобы получить ID, если нужно)
        Certificate saved = certificateRepository.save(certificate);

        // Теперь сохраняем файл и обновляем URL
        try {
            String fileUrl = saveCertificateFile(file);
            saved.setCertificateUrl(fileUrl);
            certificateRepository.save(saved); // обновляем
            log.info("Создан ручной сертификат для пользователя {} по курсу {}", user.getUsername(), course.getTitle());
        } catch (Exception e) {
            // Если сохранение файла не удалось, удаляем запись из БД (откат транзакции)
            throw new RuntimeException("Ошибка сохранения файла сертификата, операция отменена", e);
        }
    }

    private String saveCertificateFile(MultipartFile file) {
        try {
            String uploadDir = "uploads/certificates/";
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String fileName = "CERT_" + System.currentTimeMillis() + extension;
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/certificates/" + fileName;
        } catch (IOException e) {
            throw new RuntimeException("Ошибка сохранения файла сертификата", e);
        }
    }

    public Certificate getCertificateById(Long id) {
        return certificateRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new NoSuchElementException("Сертификат не найден"));
    }

    public List<Certificate> getAllCertificates() {
        return certificateRepository.findAllWithUserAndCourse();
    }

    public List<Certificate> getCertificatesByUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("Пользователь не может быть null");
        }
        return certificateRepository.findByUserWithDetails(user);
    }

    public List<Certificate> getCertificatesByCourse(Course course) {
        if (course == null) {
            throw new IllegalArgumentException("Курс не может быть null");
        }
        return certificateRepository.findByCourse(course);
    }

    public boolean isCertificateIssued(User user, Course course) {
        return certificateRepository.existsByUserAndCourse(user, course);
    }

    @Transactional
    public void revokeCertificate(Long id) {
        Certificate certificate = getCertificateById(id);
        if (certificate.isRevoked()) {
            throw new IllegalStateException("Сертификат уже отозван");
        }
        certificate.setRevoked(true);
        certificate.setRevokedDate(LocalDateTime.now());
        certificateRepository.save(certificate);
        log.info("Сертификат {} отозван", id);
    }

    @Transactional
    public void deleteCertificate(Long id) {
        Certificate certificate = getCertificateById(id);
        certificateRepository.delete(certificate);
    }

    private String generateCertificateNumber() {
        return "CERT-" + System.currentTimeMillis() + "-" + (int) (Math.random() * 1000);
    }
    public List<Certificate> getActiveCertificatesByCourse(Course course) {
        if (course == null) {
            throw new IllegalArgumentException("Курс не может быть null");
        }
        return certificateRepository.findByCourseAndRevokedFalse(course);
    }
    @Transactional
    public void deleteRevokedCertificatesForCourse(Course course) {
        certificateRepository.deleteByCourseAndRevokedTrue(course);
    }

    public Certificate findByUserAndCourse(Long userId, Long courseId) {
        return certificateRepository.findByUserIdAndCourseId(userId, courseId).orElse(null);
    }

    @Transactional
    public Certificate save(Certificate certificate) {
        return certificateRepository.save(certificate);
    }
}