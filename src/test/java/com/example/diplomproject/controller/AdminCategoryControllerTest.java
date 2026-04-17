package com.example.diplomproject.controller;

import com.example.diplomproject.entity.Category;
import com.example.diplomproject.entity.Course;
import com.example.diplomproject.service.CategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AdminCategoryControllerTest {

    @Mock
    private CategoryService categoryService;

    @InjectMocks
    private AdminCategoryController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    // ----- Тесты для GET /admin/categories -----
    @Test
    void adminList_shouldReturnListPage() throws Exception {
        when(categoryService.getAllCategories()).thenReturn(List.of(new Category(), new Category()));

        mockMvc.perform(get("/admin/categories"))
                .andExpect(status().isOk())
                .andExpect(view().name("layouts/main"))
                .andExpect(model().attributeExists("categories"))
                .andExpect(model().attribute("title", "Управление категориями"));
    }

    @Test
    void adminList_withSuccessParam_shouldAddSuccessAttribute() throws Exception {
        mockMvc.perform(get("/admin/categories").param("success", "OK"))
                .andExpect(model().attribute("success", "OK"));
    }

    // ----- Тесты для GET /admin/categories/new -----
    @Test
    void showCreateForm_shouldReturnForm() throws Exception {
        mockMvc.perform(get("/admin/categories/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("layouts/main"))
                .andExpect(model().attributeExists("category"));
    }

    // ----- Тесты для POST /admin/categories -----
    @Test
    void createCategory_success() throws Exception {
        Category saved = new Category();
        saved.setId(1L);
        when(categoryService.addNewCategory(any(Category.class))).thenReturn(saved);

        mockMvc.perform(post("/admin/categories")
                        .param("title", "New Cat"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/admin/categories*")); // игнорируем параметры

        verify(categoryService).addNewCategory(any(Category.class));
    }

    @Test
    void createCategory_withImages() throws Exception {
        Category saved = new Category();
        saved.setId(1L);
        when(categoryService.addNewCategory(any(Category.class))).thenReturn(saved);
        MockMultipartFile image = new MockMultipartFile("newImages", "img.jpg", "image/jpeg", "data".getBytes());

        mockMvc.perform(multipart("/admin/categories")
                        .file(image)
                        .param("title", "New Cat"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/admin/categories*"));

        verify(categoryService).addImagesToCategory(eq(1L), any());
    }

    @Test
    void createCategory_failure() throws Exception {
        when(categoryService.addNewCategory(any(Category.class)))
                .thenThrow(new RuntimeException("DB error"));

        mockMvc.perform(post("/admin/categories")
                        .param("title", "New Cat"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/admin/categories/new*"));
    }

    // ----- Тесты для GET /admin/categories/edit/{id} -----
    @Test
    void showEditForm_success() throws Exception {
        Category category = new Category();
        when(categoryService.getCategoryById(1L)).thenReturn(category);

        mockMvc.perform(get("/admin/categories/edit/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("layouts/main"))
                .andExpect(model().attributeExists("category"));
    }

    // ----- Тесты для POST /admin/categories/{id} -----
    @Test
    void updateCategory_success() throws Exception {
        // Предполагаем, что updateCategory возвращает Category (не void)
        Category updated = new Category();
        when(categoryService.updateCategory(eq(1L), any(Category.class))).thenReturn(updated);

        mockMvc.perform(post("/admin/categories/1")
                        .param("title", "Updated"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/admin/categories*"));

        verify(categoryService).updateCategory(eq(1L), any(Category.class));
    }

    @Test
    void updateCategory_withImages() throws Exception {
        Category updated = new Category();
        when(categoryService.updateCategory(eq(1L), any(Category.class))).thenReturn(updated);
        MockMultipartFile image = new MockMultipartFile("newImages", "img.jpg", "image/jpeg", "data".getBytes());

        mockMvc.perform(multipart("/admin/categories/1")
                        .file(image)
                        .param("title", "Updated")
                        .with(request -> { request.setMethod("POST"); return request; }))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/admin/categories*"));

        verify(categoryService).addImagesToCategory(eq(1L), any());
    }

    @Test
    void updateCategory_failure() throws Exception {
        when(categoryService.updateCategory(eq(1L), any(Category.class)))
                .thenThrow(new RuntimeException("Update error"));

        mockMvc.perform(post("/admin/categories/1")
                        .param("title", "Updated"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/admin/categories/edit/1*"));
    }

    // ----- Тесты для POST /admin/categories/delete/{id} -----
    @Test
    void deleteCategory_success() throws Exception {
        Category category = new Category();
        category.setId(1L);
        category.setCourses(Collections.emptyList());
        when(categoryService.getCategoryById(1L)).thenReturn(category);
        doNothing().when(categoryService).deleteCategoryById(1L);

        mockMvc.perform(post("/admin/categories/delete/1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/admin/categories*"));
    }

    @Test
    void deleteCategory_hasCourses_shouldFail() throws Exception {
        Category category = new Category();
        category.setId(1L);
        category.setCourses(List.of(new Course())); // есть курсы
        when(categoryService.getCategoryById(1L)).thenReturn(category);

        mockMvc.perform(post("/admin/categories/delete/1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/admin/categories*"));
    }

    @Test
    void deleteCategory_notFound() throws Exception {
        when(categoryService.getCategoryById(99L)).thenReturn(null);

        mockMvc.perform(post("/admin/categories/delete/99"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/admin/categories*"));
    }

    // ----- Тесты для управления изображениями -----
    @Test
    void deleteImage_success() throws Exception {
        doNothing().when(categoryService).deleteImage(5L);

        mockMvc.perform(post("/admin/categories/1/images/5/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/admin/categories/edit/1*"));
    }

    @Test
    void deleteImage_failure() throws Exception {
        doThrow(new RuntimeException("Image not found"))
                .when(categoryService).deleteImage(5L);

        mockMvc.perform(post("/admin/categories/1/images/5/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/admin/categories/edit/1*"));
    }

    @Test
    void setMainImage_success() throws Exception {
        doNothing().when(categoryService).setMainImage(5L);

        mockMvc.perform(post("/admin/categories/1/images/5/main"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/admin/categories/edit/1*"));
    }

    @Test
    void setMainImage_failure() throws Exception {
        doThrow(new RuntimeException("Invalid image"))
                .when(categoryService).setMainImage(5L);

        mockMvc.perform(post("/admin/categories/1/images/5/main"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/admin/categories/edit/1*"));
    }
}