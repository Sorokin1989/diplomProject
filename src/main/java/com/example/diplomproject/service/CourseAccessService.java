package com.example.diplomproject.service;

import com.example.diplomproject.entity.Course;
import com.example.diplomproject.entity.CourseAccess;
import com.example.diplomproject.entity.User;
import com.example.diplomproject.repository.CourseAccessRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class CourseAccessService {

    @Autowired
    private CourseAccessRepository courseAccessRepository;

    /**
     * Предоставление доступа к курсу пользователю
     */

    @Transactional
    public void grantAccessToCourse(User user, Course course) {
        if (user == null || course == null) {
            throw new IllegalArgumentException("Пользователь или Курс не может быть null");
        }
        if (courseAccessRepository.existsByUserAndCourse(user, course)) {
            return;
        }
        CourseAccess courseAccess = new CourseAccess();
        courseAccess.setUser(user);
        courseAccess.setCourse(course);
        courseAccess.setGrantedAt(LocalDateTime.now());
        courseAccessRepository.save(courseAccess);

    }

    /**
     * Проверка наличия доступа у пользователя к курсу
     */

    public boolean hasAccessToUser(User user, Course course) {
        if (user == null || course == null) {
            return false;
        }
        return courseAccessRepository.existsByUserAndCourse(user, course);
    }

    /**
     * Получение всех курсов, к которым есть доступ у пользователя
     */
    public List<Course> getCoursesByUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("Пользователь не может быть null");
        }
        return courseAccessRepository.findCoursesByUser(user);
    }

    /**
     * Получение всех пользователей, имеющих доступ к курсу
     */
    public List<User> getUsersByCourse(Course course) {
        if (course == null) {
            throw new IllegalArgumentException("Курс должен существовать");
        }
        return courseAccessRepository.findUsersByCourse(course);
    }

    /**
     * Удаление доступа к курсу
     */
    @Transactional
    public void revokeAccess(User user, Course course) {
        if (user == null || course == null) {
            throw new IllegalArgumentException("Пользователь и курс не могут быть null");
        }

        CourseAccess access = courseAccessRepository.findByUserAndCourse(user, course)
                .orElseThrow(() -> new NoSuchElementException("Доступ к курсу не найден"));

        courseAccessRepository.delete(access);
    }

}
