package com.example.diplomproject.mapper;

import com.example.diplomproject.dto.CertificateDto;
import com.example.diplomproject.entity.Certificate;
import com.example.diplomproject.entity.Course;
import com.example.diplomproject.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class CertificateMapperTest {

    private CertificateMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new CertificateMapper();
    }

    @Test
    void toCertificateDto_shouldMapFullCertificate() {
        // given
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");

        Course course = new Course();
        course.setId(10L);
        course.setTitle("Java for Beginners");

        Certificate certificate = new Certificate();
        certificate.setId(100L);
        certificate.setUser(user);
        certificate.setCourse(course);
        certificate.setCreatedAt(LocalDateTime.of(2025, 1, 15, 10, 30));
        certificate.setCertificateId("CERT-ABC-123");
        certificate.setCertificateUrl("/certificates/100.pdf");
        certificate.setRevoked(false);
        certificate.setRevokedDate(null);

        // when
        CertificateDto dto = mapper.toCertificateDto(certificate);

        // then
        assertNotNull(dto);
        assertEquals(100L, dto.getId());
        assertEquals(1L, dto.getUserId());
        assertEquals("testuser", dto.getUsername());
        assertEquals(10L, dto.getCourseId());
        assertEquals("Java for Beginners", dto.getCourseTitle());
        assertEquals(LocalDateTime.of(2025, 1, 15, 10, 30), dto.getCreatedAt());
        assertEquals("CERT-ABC-123", dto.getCertificateId());
        assertEquals("/certificates/100.pdf", dto.getCertificateUrl());
        assertFalse(dto.isRevoked());
        assertNull(dto.getRevokedDate());
    }

    @Test
    void toCertificateDto_shouldHandleNullUserAndCourse() {
        // given
        Certificate certificate = new Certificate();
        certificate.setId(200L);
        certificate.setUser(null);
        certificate.setCourse(null);
        certificate.setCreatedAt(null);
        certificate.setCertificateId("CERT-NULL");
        certificate.setCertificateUrl(null);
        certificate.setRevoked(true);
        certificate.setRevokedDate(LocalDateTime.now());

        // when
        CertificateDto dto = mapper.toCertificateDto(certificate);

        // then
        assertNotNull(dto);
        assertEquals(200L, dto.getId());
        assertNull(dto.getUserId());
        assertNull(dto.getUsername());
        assertNull(dto.getCourseId());
        assertNull(dto.getCourseTitle());
        assertNotNull(dto.getCreatedAt()); // установлена текущая дата
        assertEquals("CERT-NULL", dto.getCertificateId());
        assertNull(dto.getCertificateUrl());
        assertTrue(dto.isRevoked());
        assertNotNull(dto.getRevokedDate());
    }

    @Test
    void toCertificateDto_shouldReturnNullForNullInput() {
        assertNull(mapper.toCertificateDto(null));
    }
}