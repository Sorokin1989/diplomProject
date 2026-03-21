package com.example.diplomproject.service;

import com.example.diplomproject.entity.Certificate;
import com.example.diplomproject.entity.Course;
import com.example.diplomproject.entity.User;
import com.example.diplomproject.repository.CertificateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class CertificateService {

    @Autowired
    private CertificateRepository certificateRepository;

    /**
     * Генерация сертификата после покупки курса
     */
    @Transactional
    public Certificate generateCertificateForPurchase(User user, Course course) {
        if (user == null || course == null) {
            throw new IllegalArgumentException("Пользователь и курс не могут быть null");
        }

        // Проверяем, куплен ли курс и не выдан ли уже сертификат
        if (certificateRepository.existsByUserAndCourse(user, course)) {
            throw new IllegalStateException("Сертификат на этот курс уже выдан");
        }

        Certificate certificate = new Certificate();
        certificate.setUser(user);
        certificate.setCourse(course);
        certificate.setCreatedAt(LocalDateTime.now());
        certificate.setCertificateId(generateCertificateNumber());

        return certificateRepository.save(certificate);
    }

    /**
     * Получение сертификата по ID
     */
    public Certificate getCertificateById(Long id) {
        return certificateRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Сертификат не найден"));
    }

    /**
     * Получение всех сертификатов пользователя
     */
    public List<Certificate> getCertificatesByUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("Пользователь не может быть null");
        }
        return certificateRepository.findByUser(user);
    }

    /**
     * Получение всех сертификатов по курсу
     */
    public List<Certificate> getCertificatesByCourse(Course course) {
        if (course == null) {
            throw new IllegalArgumentException("Курс не может быть null");
        }
        return certificateRepository.findByCourse(course);
    }

    /**
     * Проверка, выдан ли сертификат пользователю за курс
     */
    public boolean isCertificateIssued(User user, Course course) {
        return certificateRepository.existsByUserAndCourse(user, course);
    }

    /**
     * Удаление сертификата
     */
    @Transactional
    public void deleteCertificate(Long id) {
        Certificate certificate = getCertificateById(id);
        certificateRepository.delete(certificate);
    }

    /**
     * Генерация уникального номера сертификата
     */
    private String generateCertificateNumber() {
        return "CERT-" + System.currentTimeMillis() + "-" + (int) (Math.random() * 1000);
    }
}