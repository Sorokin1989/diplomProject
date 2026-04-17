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
class CategoryControllerTest {

    @Mock
    private CategoryService categoryService;

    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void listCategories_shouldReturnViewWithCategories() throws Exception {
        // given
        Category category = new Category();
        category.setId(1L);
        category.setTitle("Test Category");
        List<Category> categories = List.of(category);

        CategoryDto categoryDto = new CategoryDto();
        categoryDto.setId(1L);
        categoryDto.setTitle("Test Category");
        List<CategoryDto> categoryDtos = List.of(categoryDto);

        when(categoryService.getAllCategories()).thenReturn(categories);
        when(categoryMapper.toCategoryDTO(any(Category.class))).thenReturn(categoryDto);

        // when & then
        mockMvc.perform(get("/categories"))
                .andExpect(status().isOk())
                .andExpect(view().name("layouts/main"))
                .andExpect(model().attributeExists("categories", "title", "content"))
                .andExpect(model().attribute("categories", categoryDtos))
                .andExpect(model().attribute("title", "Категории курсов"))
                .andExpect(model().attribute("content", "pages/categories/categories :: categories-content"));

        verify(categoryService).getAllCategories();
        verify(categoryMapper).toCategoryDTO(category);
    }

    @Test
    void listCategories_emptyList_shouldReturnEmptyList() throws Exception {
        when(categoryService.getAllCategories()).thenReturn(List.of());

        mockMvc.perform(get("/categories"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("categories", List.of()));

        verify(categoryMapper, never()).toCategoryDTO(any());
    }
}