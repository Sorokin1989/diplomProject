package com.example.diplomproject.mapper;

import com.example.diplomproject.dto.CartItemDto;
import com.example.diplomproject.entity.CartItem;
import com.example.diplomproject.entity.Course;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.assertThat;

class CartItemMapperTest {

    private CartItemMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new CartItemMapper();
    }

    @Test
    void toCartItemDTO_shouldMapFullEntity() {
        // given
        Course course = new Course();
        course.setId(10L);
        course.setTitle("Test Course");
        course.setPrice(BigDecimal.valueOf(99.99));

        CartItem cartItem = new CartItem();
        cartItem.setId(1L);
        cartItem.setCourse(course);
        cartItem.setPrice(BigDecimal.valueOf(99.99));
        cartItem.setAddedAt(LocalDateTime.of(2024, 1, 1, 12, 0));

        // when
        CartItemDto dto = mapper.toCartItemDTO(cartItem);

        // then
        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getCourseId()).isEqualTo(10L);
        assertThat(dto.getCourseTitle()).isEqualTo("Test Course");
        assertThat(dto.getPrice()).isEqualByComparingTo("99.99");
        assertThat(dto.getAddedAt()).isEqualTo("2024-01-01T12:00");
        // imageUrl может быть null, если не замокать getMainImageUrl()
    }

    @Test
    void toCartItemDTO_shouldHandleNullCourse() {
        // given
        CartItem cartItem = new CartItem();
        cartItem.setId(2L);
        cartItem.setCourse(null);
        cartItem.setPrice(null);
        cartItem.setAddedAt(null);

        // when
        CartItemDto dto = mapper.toCartItemDTO(cartItem);

        // then
        assertThat(dto).isNotNull();
        assertThat(dto.getCourseId()).isNull();
        assertThat(dto.getCourseTitle()).isEqualTo("Курс удален");
        assertThat(dto.getPrice()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(dto.getAddedAt()).isNotNull(); // установлена текущая дата
    }

    @Test
    void toCartItemDTO_shouldReturnNullForNullInput() {
        assertThat(mapper.toCartItemDTO(null)).isNull();
    }
}