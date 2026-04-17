package com.example.diplomproject.controller;

import com.example.diplomproject.entity.Certificate;
import com.example.diplomproject.entity.Course;
import com.example.diplomproject.entity.User;
import com.example.diplomproject.service.CertificateService;
import com.example.diplomproject.service.CourseService;
import com.example.diplomproject.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AdminCertificateControllerTest {

    @Mock
    private CertificateService certificateService;

    @Mock
    private UserService userService;

    @Mock
    private CourseService courseService;

    @InjectMocks
    private AdminCertificateController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    // ---------- GET /admin/certificates ----------
    @Test
    void listCertificates_shouldReturnViewWithCertificates() throws Exception {
        List<Certificate> certificates = List.of(new Certificate(), new Certificate());
        when(certificateService.getAllCertificates()).thenReturn(certificates);

        mockMvc.perform(get("/admin/certificates"))
                .andExpect(status().isOk())
                .andExpect(view().name("layouts/main"))
                .andExpect(model().attributeExists("certificates"))
                .andExpect(model().attribute("title", "Управление сертификатами"))
                .andExpect(model().attribute("content", "pages/admin/certificates/admin-list :: admin-certificates-content"));

        verify(certificateService).getAllCertificates();
    }

    // ---------- GET /admin/certificates/new ----------
    @Test
    void showCreateForm_shouldReturnForm() throws Exception {
        List<User> users = List.of(new User(), new User());
        List<Course> courses = List.of(new Course(), new Course());
        when(userService.getAllUsers()).thenReturn(users);
        when(courseService.getAllCoursesForAdmin()).thenReturn(courses);

        mockMvc.perform(get("/admin/certificates/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("layouts/main"))
                .andExpect(model().attributeExists("users", "courses"))
                .andExpect(model().attribute("title", "Создание сертификата"))
                .andExpect(model().attribute("content", "pages/admin/certificates/form :: admin-certificate-form"));

        verify(userService).getAllUsers();
        verify(courseService).getAllCoursesForAdmin();
    }

    // ---------- POST /admin/certificates/new ----------
    @Test
    void createCertificate_success() throws Exception {
        User user = new User();
        user.setId(1L);
        Course course = new Course();
        course.setId(1L);
        MockMultipartFile file = new MockMultipartFile(
                "certificateFile", "cert.pdf", "application/pdf", "dummy".getBytes());

        when(userService.getUserById(1L)).thenReturn(user);
        when(courseService.getCourseEntityById(1L)).thenReturn(course);
        doNothing().when(certificateService).createManualCertificateWithFile(any(User.class), any(Course.class), any(MultipartFile.class));

        mockMvc.perform(multipart("/admin/certificates/new")
                        .file(file)
                        .param("userId", "1")
                        .param("courseId", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/admin/certificates*"));

        verify(certificateService).createManualCertificateWithFile(any(User.class), any(Course.class), any(MultipartFile.class));
    }

    @Test
    void createCertificate_emptyFile_shouldRedirectToNewWithError() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "certificateFile", "", "application/pdf", new byte[0]);

        mockMvc.perform(multipart("/admin/certificates/new")
                        .file(emptyFile)
                        .param("userId", "1")
                        .param("courseId", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/admin/certificates/new*"));

        verify(certificateService, never()).createManualCertificateWithFile(any(), any(), any());
    }

    @Test
    void createCertificate_userNotFound_shouldRedirectToNewWithError() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "certificateFile", "cert.pdf", "application/pdf", "dummy".getBytes());
        when(userService.getUserById(1L)).thenReturn(null);

        mockMvc.perform(multipart("/admin/certificates/new")
                        .file(file)
                        .param("userId", "1")
                        .param("courseId", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/admin/certificates/new*"));

        verify(certificateService, never()).createManualCertificateWithFile(any(), any(), any());
    }

    @Test
    void createCertificate_courseNotFound_shouldRedirectToNewWithError() throws Exception {
        User user = new User();
        user.setId(1L);
        MockMultipartFile file = new MockMultipartFile(
                "certificateFile", "cert.pdf", "application/pdf", "dummy".getBytes());
        when(userService.getUserById(1L)).thenReturn(user);
        when(courseService.getCourseEntityById(1L)).thenReturn(null);

        mockMvc.perform(multipart("/admin/certificates/new")
                        .file(file)
                        .param("userId", "1")
                        .param("courseId", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/admin/certificates/new*"));

        verify(certificateService, never()).createManualCertificateWithFile(any(), any(), any());
    }

    @Test
    void createCertificate_serviceThrowsException_shouldRedirectToCertificatesList() throws Exception {
        User user = new User();
        user.setId(1L);
        Course course = new Course();
        course.setId(1L);
        MockMultipartFile file = new MockMultipartFile(
                "certificateFile", "cert.pdf", "application/pdf", "dummy".getBytes());
        when(userService.getUserById(1L)).thenReturn(user);
        when(courseService.getCourseEntityById(1L)).thenReturn(course);
        doThrow(new RuntimeException("DB error")).when(certificateService)
                .createManualCertificateWithFile(any(), any(), any());

        mockMvc.perform(multipart("/admin/certificates/new")
                        .file(file)
                        .param("userId", "1")
                        .param("courseId", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/admin/certificates*"));
    }

    // ---------- GET /admin/certificates/{id} ----------
    @Test
    void viewCertificate_found() throws Exception {
        Certificate certificate = new Certificate();
        certificate.setId(1L);
        when(certificateService.getCertificateById(1L)).thenReturn(certificate);

        mockMvc.perform(get("/admin/certificates/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("layouts/main"))
                .andExpect(model().attributeExists("certificate"))
                .andExpect(model().attribute("title", "Просмотр сертификата"))
                .andExpect(model().attribute("content", "pages/admin/certificates/view :: admin-certificate-view"));
    }

    @Test
    void viewCertificate_notFound() throws Exception {
        when(certificateService.getCertificateById(99L)).thenReturn(null);

        mockMvc.perform(get("/admin/certificates/99"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/admin/certificates*"));
    }

    // ---------- POST /admin/certificates/{id}/revoke ----------
    @Test
    void revokeCertificate_success() throws Exception {
        Certificate certificate = new Certificate();
        certificate.setId(1L);
        when(certificateService.getCertificateById(1L)).thenReturn(certificate);
        doNothing().when(certificateService).revokeCertificate(1L);

        mockMvc.perform(post("/admin/certificates/1/revoke"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/admin/certificates*"));

        verify(certificateService).revokeCertificate(1L);
    }

    @Test
    void revokeCertificate_notFound() throws Exception {
        when(certificateService.getCertificateById(99L)).thenReturn(null);

        mockMvc.perform(post("/admin/certificates/99/revoke"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/admin/certificates*"));

        verify(certificateService, never()).revokeCertificate(anyLong());
    }

    @Test
    void revokeCertificate_serviceThrowsException() throws Exception {
        Certificate certificate = new Certificate();
        certificate.setId(1L);
        when(certificateService.getCertificateById(1L)).thenReturn(certificate);
        doThrow(new RuntimeException("Revoke error")).when(certificateService).revokeCertificate(1L);

        mockMvc.perform(post("/admin/certificates/1/revoke"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/admin/certificates*"));
    }

    // ---------- POST /admin/certificates/{id}/activate ----------
    @Test
    void activateCertificate_success() throws Exception {
        doNothing().when(certificateService).activateCertificate(1L);

        mockMvc.perform(post("/admin/certificates/1/activate"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/admin/certificates*"));

        verify(certificateService).activateCertificate(1L);
    }

    @Test
    void activateCertificate_serviceThrowsException() throws Exception {
        doThrow(new RuntimeException("Activation error")).when(certificateService).activateCertificate(1L);

        mockMvc.perform(post("/admin/certificates/1/activate"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/admin/certificates*"));
    }
}