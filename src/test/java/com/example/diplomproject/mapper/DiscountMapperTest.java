package com.example.diplomproject.mapper;

import com.example.diplomproject.dto.DiscountDto;
import com.example.diplomproject.entity.Category;
import com.example.diplomproject.entity.Course;
import com.example.diplomproject.entity.Discount;
import com.example.diplomproject.enums.DiscountType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class DiscountMapperTest {

    private DiscountMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new DiscountMapper();
    }

    @Test
    void toDiscountDto_shouldMapFullDiscount() {
        // given
        Course course1 = new Course();
        course1.setId(1L);
        course1.setTitle("Course A");
        Course course2 = new Course();
        course2.setId(2L);
        course2.setTitle("Course B");

        Category cat1 = new Category();
        cat1.setId(10L);
        cat1.setTitle("Category X");
        Category cat2 = new Category();
        cat2.setId(11L);
        cat2.setTitle("Category Y");

        Discount discount = new Discount();
        discount.setId(100L);
        discount.setTitle("Summer Sale");
        discount.setDescription("Big discount");
        discount.setDiscountType(DiscountType.PERCENT);
        discount.setDiscountValue(BigDecimal.valueOf(20));
        discount.setStartDate(LocalDateTime.of(2025, 6, 1, 0, 0));
        discount.setEndDate(LocalDateTime.of(2025, 8, 31, 23, 59));
        discount.setApplicableCourses(List.of(course1, course2));
        discount.setApplicableCategories(List.of(cat1, cat2));
        discount.setMinOrderAmount(BigDecimal.valueOf(500));
        discount.setActive(true);
        discount.setCreatedAt(LocalDateTime.of(2025, 5, 1, 10, 0));

        // when
        DiscountDto dto = mapper.toDiscountDto(discount);

        // then
        assertNotNull(dto);
        assertEquals(100L, dto.getId());
        assertEquals("Summer Sale", dto.getTitle());
        assertEquals("Big discount", dto.getDescription());
        assertEquals("PERCENT", dto.getDiscountType());
        assertEquals(0, BigDecimal.valueOf(20).compareTo(dto.getDiscountValue()));
        assertEquals(LocalDateTime.of(2025, 6, 1, 0, 0), dto.getStartDate());
        assertEquals(LocalDateTime.of(2025, 8, 31, 23, 59), dto.getEndDate());
        assertNotNull(dto.getApplicableCourseIds());
        assertEquals(2, dto.getApplicableCourseIds().size());
        assertTrue(dto.getApplicableCourseIds().contains(1L));
        assertTrue(dto.getApplicableCourseIds().contains(2L));
        assertEquals(List.of("Course A", "Course B"), dto.getApplicableCourseTitles());
        assertEquals(List.of(10L, 11L), dto.getApplicableCategoryIds());
        assertEquals(List.of("Category X", "Category Y"), dto.getApplicableCategoryTitles());
        assertEquals(0, BigDecimal.valueOf(500).compareTo(dto.getMinOrderAmount()));
        assertTrue(dto.isActive());
        assertEquals(LocalDateTime.of(2025, 5, 1, 10, 0), dto.getCreatedAt());
    }

    @Test
    void toDiscountDto_shouldReturnNullForNullInput() {
        assertNull(mapper.toDiscountDto(null));
    }

    @Test
    void toDiscountDto_shouldHandleNullLists() {
        Discount discount = new Discount();
        discount.setId(200L);
        discount.setApplicableCourses(null);
        discount.setApplicableCategories(null);

        DiscountDto dto = mapper.toDiscountDto(discount);

        assertNotNull(dto);
        assertNull(dto.getApplicableCourseIds());
        assertNull(dto.getApplicableCourseTitles());
        assertNull(dto.getApplicableCategoryIds());
        assertNull(dto.getApplicableCategoryTitles());
    }
}