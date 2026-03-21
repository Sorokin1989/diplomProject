package com.example.diplomproject.service;

import com.example.diplomproject.entity.Category;
import com.example.diplomproject.entity.Course;
import com.example.diplomproject.repository.CourseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CourseService {

    @Autowired
    private CourseRepository courseRepository;


    /**
     * Получение всех курсов
     */
    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }

    /**
     * Получение курса по ID
     */
    public Course getCourseById(Long id) {
        return courseRepository.findById(id).
                orElseThrow(() ->
                        new IllegalArgumentException("Курс не найден"));
    }

    /**
     * Создание нового курса
     */
    @Transactional
    public Course createNewCourse(Course course) {
        if (course == null || course.getTitle() == null || course.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Название курса не может быть пустым");
        }
        if (course.getPrice() == null || course.getPrice().
                compareTo(java.math.BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Цена должна быть больше или равна нулю");
        }
        return courseRepository.save(course);
    }

    /**
     * Обновление курса
     */
    @Transactional
    public Course updateCourse(Course updatedCourse, Long id) {

        Course existingCourse = getCourseById(id);

        if (updatedCourse.getTitle() != null && !updatedCourse.getTitle().trim().isEmpty()) {
            existingCourse.setTitle(updatedCourse.getTitle());
        }
        if (updatedCourse.getDescription() != null) {
            existingCourse.setDescription(updatedCourse.getDescription());
        }

        if (updatedCourse.getPrice() != null && updatedCourse.getPrice().
                compareTo(BigDecimal.ZERO) >= 0) {
            existingCourse.setPrice(updatedCourse.getPrice());
        }
        if (updatedCourse.getCategory() != null) {
            existingCourse.setCategory(updatedCourse.getCategory());
        }
        return courseRepository.save(existingCourse);

    }

    /**
     * Удаление курса по ID
     */
    @Transactional
    public void deleteCourseByID(Long id) {
        Course course = getCourseById(id);
        courseRepository.delete(course);
    }

    /**
     * Получение курсов по категории
     */
    public List<Course> getByCategory(Category category) {
        if (category == null) {
            throw new IllegalArgumentException("Категория отсутствует");
        }
        return courseRepository.findByCategory(category);
    }

    /**
     * Поиск курсов по названию (частичное совпадение)
     */
    public List<Course> searchCoursesByTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            return getAllCourses();
        }
        return courseRepository.findByTitleContainingIgnoreCase(title.trim());
    }


}
