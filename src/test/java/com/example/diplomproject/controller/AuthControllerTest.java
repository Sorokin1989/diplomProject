package com.example.diplomproject.controller;

import com.example.diplomproject.dto.CategoryDto;
import com.example.diplomproject.entity.Category;
import com.example.diplomproject.mapper.CategoryMapper;
import com.example.diplomproject.service.CategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private CategoryService categoryService;

    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private AuthController authController;

    private MockMvc mockMvc;

    private List<Category> mockCategories;
    private List<CategoryDto> mockCategoryDtos;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();

        mockCategories = List.of(
                createCategory(1L, "Категория 1"),
                createCategory(2L, "Категория 2"),
                createCategory(3L, "Категория 3"),
                createCategory(4L, "Категория 4")
        );
        mockCategoryDtos = List.of(
                createCategoryDto(1L, "Категория 1"),
                createCategoryDto(2L, "Категория 2"),
                createCategoryDto(3L, "Категория 3")
        );
    }

    private Category createCategory(Long id, String title) {
        Category category = new Category();
        category.setId(id);
        category.setTitle(title);
        return category;
    }

    private CategoryDto createCategoryDto(Long id, String title) {
        CategoryDto dto = new CategoryDto();
        dto.setId(id);
        dto.setTitle(title);
        return dto;
    }

    @Test
    void loginPage_shouldReturnLoginPageWithoutErrorOrMessage() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("layouts/main"))
                .andExpect(model().attributeDoesNotExist("error"))
                .andExpect(model().attributeDoesNotExist("message"));
    }

    @Test
    void loginPage_withErrorParam_shouldAddErrorAttribute() throws Exception {
        mockMvc.perform(get("/login").param("error", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("layouts/main"))
                .andExpect(model().attribute("error", "Неверное имя пользователя или пароль"));
    }

    @Test
    void loginPage_withLogoutParam_shouldAddMessageAttribute() throws Exception {
        mockMvc.perform(get("/login").param("logout", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("layouts/main"))
                .andExpect(model().attribute("message", "Вы успешно вышли из системы"));
    }

    @Test
    void homePage_shouldReturnFirstThreeCategories() throws Exception {
        when(categoryService.getAllCategories()).thenReturn(mockCategories);
        when(categoryMapper.toCategoryDTO(any(Category.class)))
                .thenAnswer(invocation -> {
                    Category c = invocation.getArgument(0);
                    return createCategoryDto(c.getId(), c.getTitle());
                });

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("layouts/main"))
                .andExpect(model().attributeExists("categories"))
                .andExpect(model().attribute("title", "Главная"))
                .andExpect(model().attribute("categories", mockCategoryDtos));

        verify(categoryService, times(1)).getAllCategories();
        verify(categoryMapper, times(3)).toCategoryDTO(any(Category.class));
    }

    @Test
    void homePage_whenNoCategories_shouldReturnEmptyList() throws Exception {
        when(categoryService.getAllCategories()).thenReturn(List.of());

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("categories", List.of()));

        verify(categoryMapper, never()).toCategoryDTO(any());
    }

    @Test
    void homePage_whenLessThanThreeCategories_shouldReturnAll() throws Exception {
        List<Category> twoCategories = List.of(
                createCategory(1L, "Кат1"),
                createCategory(2L, "Кат2")
        );
        List<CategoryDto> twoDtos = List.of(
                createCategoryDto(1L, "Кат1"),
                createCategoryDto(2L, "Кат2")
        );
        when(categoryService.getAllCategories()).thenReturn(twoCategories);
        when(categoryMapper.toCategoryDTO(any(Category.class)))
                .thenAnswer(inv -> {
                    Category c = inv.getArgument(0);
                    return createCategoryDto(c.getId(), c.getTitle());
                });

        mockMvc.perform(get("/"))
                .andExpect(model().attribute("categories", twoDtos));

        verify(categoryMapper, times(2)).toCategoryDTO(any(Category.class));
    }
}