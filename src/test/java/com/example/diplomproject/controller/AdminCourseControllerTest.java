package com.example.diplomproject.controller;

import com.example.diplomproject.dto.CourseDto;
import com.example.diplomproject.entity.Category;
import com.example.diplomproject.entity.Certificate;
import com.example.diplomproject.entity.Course;
import com.example.diplomproject.entity.CourseImage;
import com.example.diplomproject.service.CategoryService;
import com.example.diplomproject.service.CertificateService;
import com.example.diplomproject.service.CourseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AdminCourseControllerTest {

    @Mock
    private CourseService courseService;

    @Mock
    private CategoryService categoryService;

    @Mock
    private CertificateService certificateService;

    @InjectMocks
    private AdminCourseController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    // ---------- GET /admin/courses ----------
    @Test
    void adminList_allCourses() throws Exception {
        List<Course> courses = List.of(new Course(), new Course());
        when(courseService.getAllCoursesForAdmin()).thenReturn(courses);

        mockMvc.perform(get("/admin/courses"))
                .andExpect(status().isOk())
                .andExpect(view().name("layouts/main"))
                .andExpect(model().attributeExists("courses"))
                .andExpect(model().attribute("title", "Управление курсами"))
                .andExpect(model().attribute("content", "pages/admin/courses/admin-list :: admin-courses-content"));
    }

    @Test
    void adminList_filterByCategory() throws Exception {
        Category category = new Category();
        category.setId(1L);
        category.setTitle("Test Cat");
        List<Course> filteredCourses = List.of(new Course());
        when(categoryService.getCategoryById(1L)).thenReturn(category);
        when(courseService.getCoursesByCategoryForAdmin(category)).thenReturn(filteredCourses);

        mockMvc.perform(get("/admin/courses").param("categoryId", "1"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("selectedCategory", category))
                .andExpect(model().attribute("courses", filteredCourses));
    }

    @Test
    void adminList_categoryNotFound() throws Exception {
        when(categoryService.getCategoryById(99L)).thenReturn(null);
        mockMvc.perform(get("/admin/courses").param("categoryId", "99"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("courses", Collections.emptyList()));
    }

    // ---------- GET /admin/courses/new ----------
    @Test
    void showCreateForm() throws Exception {
        List<Category> categories = List.of(new Category(), new Category());
        when(categoryService.getAllCategories()).thenReturn(categories);

        mockMvc.perform(get("/admin/courses/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("layouts/main"))
                .andExpect(model().attributeExists("course", "categories"))
                .andExpect(model().attribute("title", "Создание курса"))
                .andExpect(model().attribute("content", "pages/admin/courses/form :: course-form"));
    }

    // ---------- POST /admin/courses ----------
    @Test
    void createCourse_success() throws Exception {
        Category category = new Category();
        category.setId(1L);
        Course savedCourse = new Course();
        savedCourse.setId(10L);
        when(categoryService.getCategoryById(1L)).thenReturn(category);
        when(courseService.createNewCourse(any(Course.class))).thenReturn(savedCourse);

        mockMvc.perform(post("/admin/courses")
                        .param("title", "New Course")
                        .param("categoryId", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/admin/courses*"));
        // Не проверяем flash, так как addAttribute используется, а не addFlashAttribute
    }

    @Test
    void createCourse_noCategory() throws Exception {
        mockMvc.perform(post("/admin/courses")
                        .param("title", "New Course"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/admin/courses/new*"));
    }

    @Test
    void createCourse_categoryNotFound() throws Exception {
        when(categoryService.getCategoryById(1L)).thenReturn(null);
        mockMvc.perform(post("/admin/courses")
                        .param("categoryId", "1")
                        .param("title", "New Course"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/admin/courses/new*"));
    }

    @Test
    void createCourse_withImages() throws Exception {
        Category category = new Category();
        category.setId(1L);
        Course savedCourse = new Course();
        savedCourse.setId(10L);
        MockMultipartFile image = new MockMultipartFile("newImages", "img.jpg", "image/jpeg", "data".getBytes());
        when(categoryService.getCategoryById(1L)).thenReturn(category);
        when(courseService.createNewCourse(any(Course.class))).thenReturn(savedCourse);

        mockMvc.perform(multipart("/admin/courses")
                        .file(image)
                        .param("title", "New Course")
                        .param("categoryId", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/admin/courses*"));
    }

    @Test
    void createCourse_serviceThrowsException() throws Exception {
        Category category = new Category();
        category.setId(1L);
        when(categoryService.getCategoryById(1L)).thenReturn(category);
        when(courseService.createNewCourse(any(Course.class))).thenThrow(new RuntimeException("DB error"));

        mockMvc.perform(post("/admin/courses")
                        .param("categoryId", "1")
                        .param("title", "New Course"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/admin/courses/new*"));
    }

    // ---------- GET /admin/courses/edit/{id} ----------
    @Test
    void showEditForm() throws Exception {
        Course course = new Course();
        course.setId(1L);
        List<Category> categories = List.of(new Category());
        when(courseService.getCourseEntityById(1L)).thenReturn(course);
        when(categoryService.getAllCategories()).thenReturn(categories);

        mockMvc.perform(get("/admin/courses/edit/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("layouts/main"))
                .andExpect(model().attribute("course", course))
                .andExpect(model().attributeExists("categories"));
    }

    // ---------- POST /admin/courses/{id} ----------
    @Test
    void updateCourse_success() throws Exception {
        Category category = new Category();
        category.setId(1L);
        Course existing = new Course();
        existing.setId(1L);
        when(courseService.getCourseEntityById(1L)).thenReturn(existing);
        when(categoryService.getCategoryById(1L)).thenReturn(category);
        // Предполагаем, что updateCourse возвращает обновлённый Course
        when(courseService.updateCourse(any(Course.class), eq(1L))).thenReturn(new Course());

        mockMvc.perform(post("/admin/courses/1")
                        .param("title", "Updated")
                        .param("categoryId", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/admin/courses*"));
    }

    @Test
    void updateCourse_courseNotFound() throws Exception {
        when(courseService.getCourseEntityById(1L)).thenReturn(null);
        mockMvc.perform(post("/admin/courses/1")
                        .param("title", "Updated")
                        .param("categoryId", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/admin/courses*"));
    }

    @Test
    void updateCourse_noCategory() throws Exception {
        // В контроллере параметр categoryId обязателен. Чтобы избежать 400,
        // передаём categoryId=0 (будет интерпретировано как отсутствие категории)
        Course existing = new Course();
        existing.setId(1L);
        when(courseService.getCourseEntityById(1L)).thenReturn(existing);
        mockMvc.perform(post("/admin/courses/1")
                        .param("title", "Updated")
                        .param("categoryId", "0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/admin/courses/edit/1*"));
    }

    @Test
    void updateCourse_withImages() throws Exception {
        Category category = new Category();
        category.setId(1L);
        Course existing = new Course();
        existing.setId(1L);
        MockMultipartFile image = new MockMultipartFile("newImages", "img.jpg", "image/jpeg", "data".getBytes());
        when(courseService.getCourseEntityById(1L)).thenReturn(existing);
        when(categoryService.getCategoryById(1L)).thenReturn(category);
        when(courseService.updateCourse(any(Course.class), eq(1L))).thenReturn(new Course());

        mockMvc.perform(multipart("/admin/courses/1")
                        .file(image)
                        .param("title", "Updated")
                        .param("categoryId", "1")
                        .with(request -> { request.setMethod("POST"); return request; }))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/admin/courses*"));
    }

    @Test
    void updateCourse_failure() throws Exception {
        Category category = new Category();
        category.setId(1L);
        Course existing = new Course();
        existing.setId(1L);
        when(courseService.getCourseEntityById(1L)).thenReturn(existing);
        when(categoryService.getCategoryById(1L)).thenReturn(category);
        when(courseService.updateCourse(any(Course.class), eq(1L))).thenThrow(new RuntimeException("Update error"));

        mockMvc.perform(post("/admin/courses/1")
                        .param("title", "Updated")
                        .param("categoryId", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/admin/courses/edit/1*"));
    }

    // ---------- POST /admin/courses/delete/{id} ----------
    @Test
    void deleteCourse_success() throws Exception {
        Course course = new Course();
        course.setId(1L);
        course.setTitle("Test");
        when(courseService.getCourseEntityById(1L)).thenReturn(course);
        when(certificateService.getActiveCertificatesByCourse(course)).thenReturn(Collections.emptyList());
        doNothing().when(certificateService).deleteRevokedCertificatesForCourse(course);
        doNothing().when(courseService).deleteCourseById(1L);

        mockMvc.perform(post("/admin/courses/delete/1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/admin/courses*"));
    }

    @Test
    void deleteCourse_courseNotFound() throws Exception {
        when(courseService.getCourseEntityById(99L)).thenReturn(null);
        mockMvc.perform(post("/admin/courses/delete/99"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/admin/courses*"));
    }

    @Test
    void deleteCourse_hasActiveCertificates() throws Exception {
        Course course = new Course();
        course.setId(1L);
        when(courseService.getCourseEntityById(1L)).thenReturn(course);
        when(certificateService.getActiveCertificatesByCourse(course)).thenReturn(List.of(new Certificate(), new Certificate()));
        mockMvc.perform(post("/admin/courses/delete/1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/admin/courses*"));
    }

    @Test
    void deleteCourse_dataIntegrityViolation() throws Exception {
        Course course = new Course();
        course.setId(1L);
        when(courseService.getCourseEntityById(1L)).thenReturn(course);
        when(certificateService.getActiveCertificatesByCourse(course)).thenReturn(Collections.emptyList());
        doThrow(new DataIntegrityViolationException("FK constraint")).when(courseService).deleteCourseById(1L);
        mockMvc.perform(post("/admin/courses/delete/1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/admin/courses*"));
    }

    // ---------- POST /admin/courses/{id}/images/{imageId}/main ----------
    @Test
    void setMainImage_success() throws Exception {
        Course course = mock(Course.class);
        CourseImage courseImage = mock(CourseImage.class);
        when(courseImage.getId()).thenReturn(5L);
        when(course.getImages()).thenReturn(List.of(courseImage));
        when(courseService.getCourseEntityById(1L)).thenReturn(course);
        doNothing().when(courseService).setMainImage(5L);

        mockMvc.perform(post("/admin/courses/1/images/5/main"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/admin/courses/edit/1*"));
    }

    @Test
    void setMainImage_courseNotFound() throws Exception {
        when(courseService.getCourseEntityById(1L)).thenReturn(null);
        mockMvc.perform(post("/admin/courses/1/images/5/main"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/admin/courses*"));
    }

    @Test
    void setMainImage_imageNotBelongToCourse() throws Exception {
        Course course = mock(Course.class);
        when(course.getImages()).thenReturn(Collections.emptyList());
        when(courseService.getCourseEntityById(1L)).thenReturn(course);
        mockMvc.perform(post("/admin/courses/1/images/5/main"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/admin/courses/edit/1*"));
    }

    // ---------- GET /admin/courses/{id}/materials ----------
    @Test
    void showMaterialsForm_success() throws Exception {
        CourseDto courseDto = new CourseDto();
        courseDto.setId(1L);
        when(courseService.getCourseDtoById(1L)).thenReturn(courseDto);
        mockMvc.perform(get("/admin/courses/1/materials"))
                .andExpect(status().isOk())
                .andExpect(view().name("layouts/main"))
                .andExpect(model().attribute("course", courseDto))
                .andExpect(model().attribute("title", "Материалы курса"))
                .andExpect(model().attribute("content", "pages/admin/courses/materials :: materials-form"));
    }

    @Test
    void showMaterialsForm_courseNotFound() throws Exception {
        when(courseService.getCourseDtoById(99L)).thenThrow(new RuntimeException("Not found"));
        mockMvc.perform(get("/admin/courses/99/materials"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/courses"));
    }

    // ---------- POST /admin/courses/{id}/materials ----------
    @Test
    void uploadMaterials_success() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "materials.zip", "application/zip", "content".getBytes());
        doNothing().when(courseService).uploadCourseMaterials(eq(1L), any(MultipartFile.class));
        mockMvc.perform(multipart("/admin/courses/1/materials")
                        .file(file))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/courses/1/materials"))
                .andExpect(flash().attribute("success", "Материалы успешно загружены"));
    }

    @Test
    void uploadMaterials_failure() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "bad.zip", "application/zip", "data".getBytes());
        doThrow(new RuntimeException("Upload error")).when(courseService).uploadCourseMaterials(eq(1L), any(MultipartFile.class));
        mockMvc.perform(multipart("/admin/courses/1/materials")
                        .file(file))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/courses/1/materials"))
                .andExpect(flash().attribute("error", "Ошибка загрузки: Upload error"));
    }

    // ---------- POST /admin/courses/{id}/materials/delete ----------
    @Test
    void deleteMaterials_success() throws Exception {
        doNothing().when(courseService).deleteCourseMaterials(1L);
        mockMvc.perform(post("/admin/courses/1/materials/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/courses/1/materials"))
                .andExpect(flash().attribute("success", "Материалы удалены"));
    }

    @Test
    void deleteMaterials_failure() throws Exception {
        doThrow(new RuntimeException("Delete error")).when(courseService).deleteCourseMaterials(1L);
        mockMvc.perform(post("/admin/courses/1/materials/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/courses/1/materials"))
                .andExpect(flash().attribute("error", "Ошибка удаления: Delete error"));
    }
}