package com.example.diplomproject.service;

import com.example.diplomproject.entity.Course;
import com.example.diplomproject.entity.CourseAccess;
import com.example.diplomproject.entity.Order;
import com.example.diplomproject.entity.User;
import com.example.diplomproject.repository.CourseAccessRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseAccessServiceTest {

    @Mock
    private CourseAccessRepository courseAccessRepository;

    @InjectMocks
    private CourseAccessService courseAccessService;

    private User user;
    private Course course;
    private Order order;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        course = new Course();
        course.setId(10L);
        order = new Order();
        order.setId(100L);
    }

    @Test
    void grantAccessToCourse_shouldCreateNewAccess() {
        when(courseAccessRepository.existsByUserAndCourse(user, course)).thenReturn(false);
        courseAccessService.grantAccessToCourse(user, course, order);
        verify(courseAccessRepository).save(any(CourseAccess.class));
    }

    @Test
    void grantAccessToCourse_shouldNotCreateIfAlreadyExists() {
        when(courseAccessRepository.existsByUserAndCourse(user, course)).thenReturn(true);
        courseAccessService.grantAccessToCourse(user, course, order);
        verify(courseAccessRepository, never()).save(any());
    }

    @Test
    void grantAccessToCourse_shouldThrowWhenUserOrCourseNull() {
        assertThatThrownBy(() -> courseAccessService.grantAccessToCourse(null, course, order))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> courseAccessService.grantAccessToCourse(user, null, order))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void grantAccessToCourse_shouldThrowWhenOrderNull() {
        assertThatThrownBy(() -> courseAccessService.grantAccessToCourse(user, course, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void revokeAccess_shouldDeleteWhenExists() {
        CourseAccess access = new CourseAccess();
        when(courseAccessRepository.findByUserAndCourse(user, course)).thenReturn(Optional.of(access));
        courseAccessService.revokeAccess(user, course);
        verify(courseAccessRepository).delete(access);
    }

    @Test
    void revokeAccess_shouldThrowWhenNotFound() {
        when(courseAccessRepository.findByUserAndCourse(user, course)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> courseAccessService.revokeAccess(user, course))
                .isInstanceOf(NoSuchElementException.class);
        verify(courseAccessRepository, never()).delete(any());
    }
}