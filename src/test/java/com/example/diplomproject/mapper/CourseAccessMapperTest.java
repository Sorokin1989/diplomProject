package com.example.diplomproject.mapper;

import com.example.diplomproject.dto.CourseAccessDto;
import com.example.diplomproject.entity.Course;
import com.example.diplomproject.entity.CourseAccess;
import com.example.diplomproject.entity.Order;
import com.example.diplomproject.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class CourseAccessMapperTest {

    private CourseAccessMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new CourseAccessMapper();
    }

    @Test
    void toCourseAccessDto_shouldMapFullEntity() {
        // given
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");

        Course course = new Course();
        course.setId(10L);
        course.setTitle("Test Course");

        Order order = new Order();
        order.setId(100L);

        CourseAccess access = new CourseAccess();
        access.setId(200L);
        access.setUser(user);
        access.setCourse(course);
        access.setOrder(order);
        access.setGrantedAt(LocalDateTime.of(2025, 1, 1, 10, 0));
        access.setExpiresAt(LocalDateTime.of(2026, 1, 1, 10, 0));
        access.setActive(true);
        access.setCreatedAt(LocalDateTime.of(2025, 1, 1, 9, 0));

        // when
        CourseAccessDto dto = mapper.toCourseAccessDto(access);

        // then
        assertNotNull(dto);
        assertEquals(200L, dto.getId());
        assertEquals(1L, dto.getUserId());
        assertEquals("testuser", dto.getUsername());
        assertEquals(10L, dto.getCourseId());
        assertEquals("Test Course", dto.getCourseTitle());
        assertEquals(100L, dto.getOrderId());
        assertEquals(LocalDateTime.of(2025, 1, 1, 10, 0), dto.getGrantedAt());
        assertEquals(LocalDateTime.of(2026, 1, 1, 10, 0), dto.getExpiresAt());
        assertTrue(dto.isActive());
        assertEquals(LocalDateTime.of(2025, 1, 1, 9, 0), dto.getCreatedAt());
    }

    @Test
    void toCourseAccessDto_shouldHandleNullRelations() {
        // given
        CourseAccess access = new CourseAccess();
        access.setId(300L);
        access.setUser(null);
        access.setCourse(null);
        access.setOrder(null);
        access.setGrantedAt(null);
        access.setExpiresAt(null);
        access.setActive(false);
        access.setCreatedAt(null);

        // when
        CourseAccessDto dto = mapper.toCourseAccessDto(access);

        // then
        assertNotNull(dto);
        assertNull(dto.getUserId());
        assertNull(dto.getUsername());
        assertNull(dto.getCourseId());
        assertNull(dto.getCourseTitle());
        assertNull(dto.getOrderId());
        assertNull(dto.getGrantedAt());
        assertNull(dto.getExpiresAt());
        assertFalse(dto.isActive());
        assertNull(dto.getCreatedAt());
    }

    @Test
    void toCourseAccessDto_shouldReturnNullForNullInput() {
        assertNull(mapper.toCourseAccessDto(null));
    }
}