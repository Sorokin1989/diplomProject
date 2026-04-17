package com.example.diplomproject.mapper;

import com.example.diplomproject.dto.OrderItemDto;
import com.example.diplomproject.entity.Course;
import com.example.diplomproject.entity.OrderItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class OrderItemMapperTest {

    private OrderItemMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new OrderItemMapper();
    }

    @Test
    void toOrderItemDto_shouldMapFullOrderItem() {
        // given
        Course course = new Course();
        course.setId(10L);
        course.setTitle("Test Course");

        OrderItem item = new OrderItem();
        item.setId(1L);
        item.setPrice(BigDecimal.valueOf(99.99));
        item.setCourse(course);
        item.setCourseTitle("Test Course");

        // when
        OrderItemDto dto = mapper.toOrderItemDto(item);

        // then
        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals(0, BigDecimal.valueOf(99.99).compareTo(dto.getPrice()));
        assertEquals(10L, dto.getCourseId());
        assertEquals("Test Course", dto.getCourseTitle());
    }

    @Test
    void toOrderItemDto_shouldHandleNullCourse() {
        // given
        OrderItem item = new OrderItem();
        item.setId(2L);
        item.setPrice(BigDecimal.valueOf(49.99));
        item.setCourse(null);
        item.setCourseTitle(null);

        // when
        OrderItemDto dto = mapper.toOrderItemDto(item);

        // then
        assertNotNull(dto);
        assertNull(dto.getCourseId());
        assertEquals("Курс удален", dto.getCourseTitle());
        assertEquals(0, BigDecimal.valueOf(49.99).compareTo(dto.getPrice()));
    }

    @Test
    void toOrderItemDto_shouldHandleNullPrice() {
        // given
        OrderItem item = new OrderItem();
        item.setId(3L);
        item.setPrice(null);
        item.setCourse(null);

        // when
        OrderItemDto dto = mapper.toOrderItemDto(item);

        // then
        assertNotNull(dto);
        assertNull(dto.getPrice()); // или ZERO, если добавить защиту
    }

    @Test
    void toOrderItemDto_shouldReturnNullForNullInput() {
        assertNull(mapper.toOrderItemDto(null));
    }
}