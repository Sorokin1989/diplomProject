package com.example.diplomproject.mapper;

import com.example.diplomproject.dto.CourseDto;
import com.example.diplomproject.entity.Category;
import com.example.diplomproject.entity.Course;
import com.example.diplomproject.entity.CourseImage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class CourseMapperTest {

    private CourseMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new CourseMapper();
    }

    @Test
    void toCourseDto_shouldMapFullCourse() {
        // given
        Category category = new Category();
        category.setId(5L);

        Course course = new Course();
        course.setId(1L);
        course.setTitle("Spring Boot Masterclass");
        course.setDescription("Deep dive into Spring Boot");
        course.setPrice(BigDecimal.valueOf(199.99));
        course.setAuthor("John Doe");
        course.setMaterialsPath("/materials/spring.zip");
        course.setCategory(category);
        course.setCreatedAt(LocalDateTime.of(2025, 1, 1, 12, 0));
        course.setReviewCount(42);

        CourseImage mainImage = new CourseImage();
        mainImage.setMain(true);
        mainImage.setFilePath("/img/main.jpg");
        CourseImage secondImage = new CourseImage();
        secondImage.setMain(false);
        secondImage.setFilePath("/img/second.jpg");
        course.setImages(List.of(mainImage, secondImage));

        // when
        CourseDto dto = mapper.toCourseDto(course);

        // then
        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals("Spring Boot Masterclass", dto.getTitle());
        assertEquals("Deep dive into Spring Boot", dto.getDescription());
        assertEquals(0, BigDecimal.valueOf(199.99).compareTo(dto.getPrice()));
        assertEquals("John Doe", dto.getAuthor());
        assertEquals("/materials/spring.zip", dto.getMaterialsPath());
        assertEquals(5L, dto.getCategoryId());
        assertEquals("/img/main.jpg", dto.getImageUrl());
        assertEquals(LocalDateTime.of(2025, 1, 1, 12, 0), dto.getCreatedAt());
        assertEquals(42, dto.getReviewCount());
        // Если раскомментировать маппинг imageUrls, то можно проверить:
        // assertNotNull(dto.getImageUrls());
        // assertEquals(2, dto.getImageUrls().size());
    }

    @Test
    void toCourseDto_shouldHandleNullCategory() {
        // given
        Course course = new Course();
        course.setId(2L);
        course.setCategory(null);
        course.setImages(List.of());

        // when
        CourseDto dto = mapper.toCourseDto(course);

        // then
        assertNotNull(dto);
        assertNull(dto.getCategoryId());
    }

    @Test
    void toCourseDto_shouldReturnNullForNullInput() {
        assertNull(mapper.toCourseDto(null));
    }
}