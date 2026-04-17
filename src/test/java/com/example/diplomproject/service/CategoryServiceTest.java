package com.example.diplomproject.service;

import com.example.diplomproject.dto.CategoryDto;
import com.example.diplomproject.entity.Category;
import com.example.diplomproject.entity.CategoryImage;
import com.example.diplomproject.mapper.CategoryMapper;
import com.example.diplomproject.repository.CategoryImageRepository;
import com.example.diplomproject.repository.CategoryRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private CategoryImageRepository categoryImageRepository;
    @Mock
    private FileStorageService fileStorageService;
    @Mock
    private CategoryMapper categoryMapper;
    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private CategoryService categoryService;

    private Category category;
    private CategoryImage categoryImage;

    @BeforeEach
    void setUp() {
        category = new Category();
        category.setId(1L);
        category.setTitle("Test Category");
        category.setDescription("Test Description");

        categoryImage = new CategoryImage();
        categoryImage.setId(10L);
        categoryImage.setFilePath("/uploads/test.jpg");
        categoryImage.setMain(true);
        categoryImage.setCategory(category);

        // ✅ Создаём изменяемый список
        List<CategoryImage> images = new ArrayList<>();
        images.add(categoryImage);
        category.setImages(images);
        ReflectionTestUtils.setField(categoryService, "entityManager", entityManager);
    }

    // ========== getCategoryById ==========
    @Test
    void getCategoryById_shouldReturnCategoryWhenExists() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        Category found = categoryService.getCategoryById(1L);
        assertThat(found).isEqualTo(category);
        verify(categoryRepository).findById(1L);
    }

    @Test
    void getCategoryById_shouldThrowWhenNotFound() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> categoryService.getCategoryById(99L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("Категория не найдена");
    }

    // ========== addNewCategory ==========
    @Test
    void addNewCategory_shouldSaveWhenValid() {
        when(categoryRepository.existsByTitleContainingIgnoreCase("Test Category")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(category);
        Category saved = categoryService.addNewCategory(category);
        assertThat(saved).isEqualTo(category);
        verify(categoryRepository).save(category);
    }

    @Test
    void addNewCategory_shouldThrowWhenTitleEmpty() {
        category.setTitle("");
        assertThatThrownBy(() -> categoryService.addNewCategory(category))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Название категории не может быть пустым");
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void addNewCategory_shouldThrowWhenDuplicateTitle() {
        when(categoryRepository.existsByTitleContainingIgnoreCase("Test Category")).thenReturn(true);
        assertThatThrownBy(() -> categoryService.addNewCategory(category))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Такая категория уже существует");
        verify(categoryRepository, never()).save(any());
    }

    // ========== updateCategory ==========
    @Test
    void updateCategory_shouldUpdateWhenValid() {
        Category updated = new Category();
        updated.setTitle("New Title");
        updated.setDescription("New Desc");

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryRepository.existsByTitleIgnoreCase("New Title")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(category);

        Category result = categoryService.updateCategory(1L, updated);
        assertThat(result.getTitle()).isEqualTo("New Title");
        assertThat(result.getDescription()).isEqualTo("New Desc");
        verify(categoryRepository).save(category);
    }

    @Test
    void updateCategory_shouldThrowWhenTitleEmpty() {
        // given
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        Category updated = new Category();
        updated.setTitle("");

        // when/then
        assertThatThrownBy(() -> categoryService.updateCategory(1L, updated))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Название категории не может быть пустым");
    }

    // ========== deleteCategoryById ==========
    @Test
    void deleteCategoryById_shouldDeleteAndRemoveFiles() throws IOException {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        doNothing().when(fileStorageService).deleteFile(anyString());

        categoryService.deleteCategoryById(1L);

        verify(categoryRepository).delete(category);
        verify(fileStorageService).deleteFile("/uploads/test.jpg");
    }

    // ========== setMainImage ==========
    @Test
    void setMainImage_shouldUpdateMainFlag() {
        // Создаём второе изображение
        CategoryImage newMain = new CategoryImage();
        newMain.setId(20L);
        newMain.setMain(false);
        newMain.setCategory(category);

        // ✅ Создаём изменяемый список с двумя изображениями
        List<CategoryImage> images = new ArrayList<>();
        images.add(categoryImage);
        images.add(newMain);
        category.setImages(images);

        when(categoryImageRepository.findById(20L)).thenReturn(Optional.of(newMain));
        when(categoryImageRepository.saveAll(anyList())).thenReturn(List.of());

        categoryService.setMainImage(20L);

        assertThat(newMain.isMain()).isTrue();
        assertThat(categoryImage.isMain()).isFalse();
        verify(categoryImageRepository).saveAll(anyList());
    }

    // ========== deleteImage ==========
    @Test
    void deleteImage_shouldDeleteFileAndImage() throws IOException {
        when(categoryImageRepository.findById(10L)).thenReturn(Optional.of(categoryImage));
        doNothing().when(fileStorageService).deleteFile(anyString());
        categoryService.deleteImage(10L);

        verify(fileStorageService).deleteFile("/uploads/test.jpg");
        verify(categoryImageRepository).delete(categoryImage);
    }

    // ========== getAllCategoriesDto ==========
    @Test
    void getAllCategoriesDto_shouldReturnMappedList() {
        when(categoryRepository.findAll()).thenReturn(List.of(category));
        when(categoryMapper.toCategoryDTO(category)).thenReturn(new CategoryDto());
        List<CategoryDto> dtos = categoryService.getAllCategoriesDto();
        assertThat(dtos).hasSize(1);
        verify(categoryMapper).toCategoryDTO(category);
    }
}