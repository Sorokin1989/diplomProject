package com.example.diplomproject.mapper;

import com.example.diplomproject.dto.CategoryDto;
import com.example.diplomproject.dto.CourseDto;
import com.example.diplomproject.entity.Category;
import com.example.diplomproject.entity.CategoryImage;
import com.example.diplomproject.entity.Course;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryMapperTest {

    @Mock
    private CourseMapper courseMapper;

    @InjectMocks
    private CategoryMapper categoryMapper;

    @Test
    void toCategoryDTO_shouldMapFullCategory() {
        // given
        Category category = new Category();
        category.setId(1L);
        category.setTitle("Test");
        category.setDescription("Desc");
        CategoryImage image = new CategoryImage();
        image.setMain(true);
        image.setFilePath("/img.jpg");
        category.setImages(List.of(image));

        Course course = new Course();
        course.setId(10L);
        category.setCourses(List.of(course));

        when(courseMapper.toCourseDto(course)).thenReturn(new CourseDto());

        // when
        CategoryDto dto = categoryMapper.toCategoryDTO(category);

        // then
        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getTitle()).isEqualTo("Test");
        assertThat(dto.getImageUrl()).isEqualTo("/img.jpg");
        assertEquals(1, dto.getCourseDtos().size());
    }

    @Test
    void toCategoryDTO_shouldReturnNullForNullInput() {
        assertThat(categoryMapper.toCategoryDTO(null)).isNull();
    }
}