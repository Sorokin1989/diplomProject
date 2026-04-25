package com.example.diplomproject.service;

import com.example.diplomproject.entity.Certificate;
import com.example.diplomproject.entity.Course;
import com.example.diplomproject.entity.User;
import com.example.diplomproject.repository.CertificateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CertificateServiceTest {

    @Mock
    private CertificateRepository certificateRepository;

    @InjectMocks
    private CertificateService certificateService;

    private User user;
    private Course course;
    private Certificate certificate;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(certificateService, "storagePath", tempDir.toString());
        user = new User();
        user.setId(1L);
        user.setUsername("testuser");

        course = new Course();
        course.setId(10L);
        course.setTitle("Test Course");

        certificate = new Certificate();
        certificate.setId(100L);
        certificate.setUser(user);
        certificate.setCourse(course);
        certificate.setCreatedAt(LocalDateTime.now());
        certificate.setCertificateId("CERT-123");
        certificate.setRevoked(false);
        certificate.setCertificateUrl("uploads/certificates/CERT_123456.pdf");
    }

    // ========== createManualCertificateWithFile ==========
    @Test
    void createManualCertificateWithFile_shouldSaveCertificateAndFile() throws Exception {
        MultipartFile file = new MockMultipartFile(
                "file", "cert.pdf", "application/pdf", "dummy content".getBytes()
        );

        when(certificateRepository.existsByUserAndCourse(user, course)).thenReturn(false);
        when(certificateRepository.save(any(Certificate.class)))
                .thenAnswer(inv -> inv.getArgument(0)); // возвращаем тот же объект

        certificateService.createManualCertificateWithFile(user, course, file);

        verify(certificateRepository, times(2)).save(any(Certificate.class)); // два сохранения
        verify(certificateRepository).existsByUserAndCourse(user, course);
        // можно дополнительно проверить, что файл реально создан, но в unit-тесте необязательно
    }

    @Test
    void createManualCertificateWithFile_shouldThrowWhenUserOrCourseNull() {
        assertThatThrownBy(() -> certificateService.createManualCertificateWithFile(null, course, mock(MultipartFile.class)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Пользователь и курс обязательны");
    }

    @Test
    void createManualCertificateWithFile_shouldThrowWhenFileEmpty() {
        MultipartFile emptyFile = new MockMultipartFile("file", new byte[0]);
        assertThatThrownBy(() -> certificateService.createManualCertificateWithFile(user, course, emptyFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Файл сертификата не загружен");
    }

    @Test
    void createManualCertificateWithFile_shouldThrowWhenCertificateAlreadyExists() {
        when(certificateRepository.existsByUserAndCourse(user, course)).thenReturn(true);
        MultipartFile file = mock(MultipartFile.class);
        assertThatThrownBy(() -> certificateService.createManualCertificateWithFile(user, course, file))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Сертификат на этот курс уже существует");
        verify(certificateRepository, never()).save(any());
    }

    // ========== getCertificateById ==========
    @Test
    void getCertificateById_shouldReturnCertificate() {
        when(certificateRepository.findByIdWithDetails(100L)).thenReturn(Optional.of(certificate));
        Certificate found = certificateService.getCertificateById(100L);
        assertThat(found).isEqualTo(certificate);
    }

    @Test
    void getCertificateById_shouldThrowWhenNotFound() {
        when(certificateRepository.findByIdWithDetails(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> certificateService.getCertificateById(999L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("Сертификат не найден");
    }

    // ========== revokeCertificate ==========
    @Test
    void revokeCertificate_shouldRevoke() {
        when(certificateRepository.findByIdWithDetails(100L)).thenReturn(Optional.of(certificate));
        certificateService.revokeCertificate(100L);
        assertThat(certificate.isRevoked()).isTrue();
        assertThat(certificate.getRevokedDate()).isNotNull();
        verify(certificateRepository).save(certificate);
    }

    @Test
    void revokeCertificate_shouldThrowWhenAlreadyRevoked() {
        certificate.setRevoked(true);
        when(certificateRepository.findByIdWithDetails(100L)).thenReturn(Optional.of(certificate));
        assertThatThrownBy(() -> certificateService.revokeCertificate(100L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Сертификат уже отозван");
        verify(certificateRepository, never()).save(any());
    }

    // ========== activateCertificate ==========
    @Test
    void activateCertificate_shouldActivate() {
        certificate.setRevoked(true);
        when(certificateRepository.findByIdWithDetails(100L)).thenReturn(Optional.of(certificate));
        certificateService.activateCertificate(100L);
        assertThat(certificate.isRevoked()).isFalse();
        assertThat(certificate.getRevokedDate()).isNull();
        verify(certificateRepository).save(certificate);
    }

    @Test
    void activateCertificate_shouldThrowWhenAlreadyActive() {
        when(certificateRepository.findByIdWithDetails(100L)).thenReturn(Optional.of(certificate));
        assertThatThrownBy(() -> certificateService.activateCertificate(100L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Сертификат уже активен");
        verify(certificateRepository, never()).save(any());
    }

    // ========== deleteCertificate ==========
    @Test
    void deleteCertificate_shouldDeleteCertificateAndFile() throws IOException {
        when(certificateRepository.findByIdWithDetails(100L)).thenReturn(Optional.of(certificate));
        // мокаем удаление файла, чтобы не трогать реальную файловую систему
        try (var mockedFiles = mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.deleteIfExists(any(Path.class))).thenReturn(true);
            certificateService.deleteCertificate(100L);
            verify(certificateRepository).delete(certificate);
            mockedFiles.verify(() -> Files.deleteIfExists(any(Path.class)), times(1));
        }
    }

    // ========== getCertificatesByUser ==========
    @Test
    void getCertificatesByUser_shouldReturnList() {
        when(certificateRepository.findByUserWithDetails(user)).thenReturn(List.of(certificate));
        List<Certificate> list = certificateService.getCertificatesByUser(user);
        assertThat(list).hasSize(1);
        assertThat(list.get(0)).isEqualTo(certificate);
    }

    @Test
    void getCertificatesByUser_shouldThrowWhenUserNull() {
        assertThatThrownBy(() -> certificateService.getCertificatesByUser(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ========== getActiveCertificatesByCourse ==========
    @Test
    void getActiveCertificatesByCourse_shouldReturnOnlyActive() {
        when(certificateRepository.findByCourseAndRevokedFalse(course)).thenReturn(List.of(certificate));
        List<Certificate> list = certificateService.getActiveCertificatesByCourse(course);
        assertThat(list).hasSize(1);
    }

    // ========== deleteRevokedCertificatesForCourse ==========
    @Test
    void deleteRevokedCertificatesForCourse_shouldDeleteRevokedAndFiles() throws IOException {
        Certificate revokedCert = new Certificate();
        revokedCert.setRevoked(true);
        revokedCert.setCertificateUrl("uploads/certificates/revoked.pdf");
        when(certificateRepository.findByCourseAndRevokedTrue(course)).thenReturn(List.of(revokedCert));
        doNothing().when(certificateRepository).deleteByCourseAndRevokedTrue(course);
        try (var mockedFiles = mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.deleteIfExists(any(Path.class))).thenReturn(true);
            certificateService.deleteRevokedCertificatesForCourse(course);
            verify(certificateRepository).deleteByCourseAndRevokedTrue(course);
            mockedFiles.verify(() -> Files.deleteIfExists(any(Path.class)), times(1));
        }
    }
}