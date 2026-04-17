package com.example.diplomproject.controller;

import com.example.diplomproject.dto.CertificateDto;
import com.example.diplomproject.entity.Certificate;
import com.example.diplomproject.entity.User;
import com.example.diplomproject.enums.Role;
import com.example.diplomproject.mapper.CertificateMapper;
import com.example.diplomproject.service.CertificateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CertificateControllerTest {

    @Mock
    private CertificateService certificateService;

    @Mock
    private CertificateMapper certificateMapper;

    @InjectMocks
    private CertificateController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    private void authenticateUser(User user) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities())
        );
    }

    private void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    // ---------- GET /certificates ----------
    @Test
    void listUserCertificates_authenticated_shouldReturnCertificates() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setRole(Role.USER);
        authenticateUser(user);

        Certificate cert = new Certificate();
        cert.setId(1L);
        CertificateDto dto = new CertificateDto();
        dto.setId(1L);
        when(certificateService.getCertificatesByUser(user)).thenReturn(List.of(cert));
        when(certificateMapper.toCertificateDto(cert)).thenReturn(dto);

        mockMvc.perform(get("/certificates"))
                .andExpect(status().isOk())
                .andExpect(view().name("layouts/main"))
                .andExpect(model().attributeExists("certificates", "title", "content"))
                .andExpect(model().attribute("certificates", List.of(dto)))
                .andExpect(model().attribute("title", "Мои сертификаты"))
                .andExpect(model().attribute("content", "pages/certificates/list :: certificates-list-content"));

        clearAuthentication();
    }

    @Test
    void listUserCertificates_unauthenticated_shouldRedirectToLogin() throws Exception {
        mockMvc.perform(get("/certificates"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    // ---------- GET /certificates/{id} ----------
    @Test
    void viewCertificate_owner_shouldReturnCertificate() throws Exception {
        User owner = new User();
        owner.setId(1L);
        owner.setRole(Role.USER);
        authenticateUser(owner);

        Certificate cert = new Certificate();
        cert.setId(1L);
        User certUser = new User();
        certUser.setId(1L);
        cert.setUser(certUser);
        CertificateDto dto = new CertificateDto();
        dto.setId(1L);
        when(certificateService.getCertificateById(1L)).thenReturn(cert);
        when(certificateMapper.toCertificateDto(cert)).thenReturn(dto);

        mockMvc.perform(get("/certificates/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("layouts/main"))
                .andExpect(model().attributeExists("certificate", "title", "content"))
                .andExpect(model().attribute("title", "Сертификат"))
                .andExpect(model().attribute("content", "pages/certificates/view :: certificate-view-content"));

        clearAuthentication();
    }

    @Test
    void viewCertificate_admin_shouldReturnCertificate() throws Exception {
        User admin = new User();
        admin.setId(2L);
        admin.setRole(Role.ADMIN);
        authenticateUser(admin);

        Certificate cert = new Certificate();
        cert.setId(1L);
        User owner = new User();
        owner.setId(1L);
        cert.setUser(owner);
        CertificateDto dto = new CertificateDto();
        dto.setId(1L);
        when(certificateService.getCertificateById(1L)).thenReturn(cert);
        when(certificateMapper.toCertificateDto(cert)).thenReturn(dto);

        mockMvc.perform(get("/certificates/1"))
                .andExpect(status().isOk());

        clearAuthentication();
    }

    @Test
    void viewCertificate_notOwnerAndNotAdmin_shouldReturnForbidden() throws Exception {
        User stranger = new User();
        stranger.setId(3L);
        stranger.setRole(Role.USER);
        authenticateUser(stranger);

        Certificate cert = new Certificate();
        cert.setId(1L);
        User owner = new User();
        owner.setId(1L);
        cert.setUser(owner);
        when(certificateService.getCertificateById(1L)).thenReturn(cert);

        mockMvc.perform(get("/certificates/1"))
                .andExpect(status().isForbidden());

        clearAuthentication();
    }

    @Test
    void viewCertificate_unauthenticated_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/certificates/1"))
                .andExpect(status().isUnauthorized());
    }

    // ---------- GET /certificates/download/{courseId} ----------
    // Тесты для успешного скачивания сложны из-за реального файла, поэтому тестируем только ошибки
    @Test
    void downloadCertificate_userNotFound_shouldReturnUnauthorized() {
        assertThrows(ResponseStatusException.class, () -> {
            controller.downloadCertificate(1L, null);
        });
    }

    @Test
    void downloadCertificate_certificateNotFound_shouldReturnNotFound() {
        User user = new User();
        user.setId(1L);
        when(certificateService.findByUserAndCourse(1L, 1L)).thenReturn(null);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            controller.downloadCertificate(1L, user);
        });
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void downloadCertificate_revoked_shouldReturnNotFound() {
        User user = new User();
        user.setId(1L);
        Certificate cert = new Certificate();
        cert.setRevoked(true);
        when(certificateService.findByUserAndCourse(1L, 1L)).thenReturn(cert);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            controller.downloadCertificate(1L, user);
        });
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void downloadCertificate_nullUrl_shouldReturnNotFound() {
        User user = new User();
        user.setId(1L);
        Certificate cert = new Certificate();
        cert.setRevoked(false);
        cert.setCertificateUrl(null);
        when(certificateService.findByUserAndCourse(1L, 1L)).thenReturn(cert);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            controller.downloadCertificate(1L, user);
        });
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}