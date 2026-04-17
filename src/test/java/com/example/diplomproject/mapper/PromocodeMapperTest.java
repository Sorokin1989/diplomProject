package com.example.diplomproject.mapper;

import com.example.diplomproject.dto.PromocodeDto;
import com.example.diplomproject.entity.Category;
import com.example.diplomproject.entity.Course;
import com.example.diplomproject.entity.Promocode;
import com.example.diplomproject.enums.DiscountType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PromocodeMapperTest {

    @Mock
    private CourseMapper courseMapper;

    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private PromocodeMapper promocodeMapper;

    private Promocode promocode;
    private Course course;
    private Category category;

    @BeforeEach
    void setUp() {
        course = new Course();
        course.setId(1L);
        course.setTitle("Test Course");

        category = new Category();
        category.setId(10L);
        category.setTitle("Test Category");

        promocode = new Promocode();
        promocode.setId(100L);
        promocode.setCode("SAVE20");
        promocode.setDiscountType(DiscountType.PERCENT);
        promocode.setValue(BigDecimal.valueOf(20));
        promocode.setMinOrderAmount(BigDecimal.valueOf(500));
        promocode.setValidFrom(LocalDateTime.of(2025, 1, 1, 0, 0));
        promocode.setValidTo(LocalDateTime.of(2025, 12, 31, 23, 59));
        promocode.setUsageLimit(100);
        promocode.setUsedCount(5);
        promocode.setApplicableCourses(List.of(course));
        promocode.setApplicableCategories(List.of(category));
        promocode.setActive(true);
        promocode.setCreatedAt(LocalDateTime.of(2024, 12, 1, 10, 0));
    }

    @Test
    void toPromoCodeDTO_shouldMapFullPromocode() {
        when(courseMapper.toCourseDto(any())).thenReturn(null);
        when(categoryMapper.toCategoryDTO(any())).thenReturn(null);

        PromocodeDto dto = promocodeMapper.toPromoCodeDTO(promocode);

        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(100L);
        assertThat(dto.getCode()).isEqualTo("SAVE20");
        assertThat(dto.getDiscountType()).isEqualTo("PERCENT");
        assertThat(dto.getValue()).isEqualByComparingTo("20");
        assertThat(dto.getMinOrderAmount()).isEqualByComparingTo("500");
        assertThat(dto.getValidFrom()).isEqualTo(LocalDateTime.of(2025, 1, 1, 0, 0));
        assertThat(dto.getValidTo()).isEqualTo(LocalDateTime.of(2025, 12, 31, 23, 59));
        assertThat(dto.getUsageLimit()).isEqualTo(100);
        assertThat(dto.getUsedCount()).isEqualTo(5);
        assertThat(dto.getApplicableCourseDtos()).hasSize(1);
        assertThat(dto.getApplicableCategoryDtos()).hasSize(1);
        assertThat(dto.isActive()).isTrue();
        assertThat(dto.getCreatedAt()).isEqualTo(LocalDateTime.of(2024, 12, 1, 10, 0));
    }

    @Test
    void toPromoCodeDTO_shouldReturnNullForNullInput() {
        assertThat(promocodeMapper.toPromoCodeDTO(null)).isNull();
    }
}