package com.example.diplomproject.service;

import com.example.diplomproject.entity.Category;
import com.example.diplomproject.entity.Course;
import com.example.diplomproject.entity.CourseImage;
import com.example.diplomproject.repository.CourseImageRepository;
import com.example.diplomproject.repository.CourseRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CourseService {
    @PersistenceContext
    private EntityManager entityManager;

    private static final Logger log = LoggerFactory.getLogger(CourseService.class);

    private final CourseRepository courseRepository;
    private final CourseImageRepository courseImageRepository;
    private final FileStorageService fileStorageService;

    @Autowired
    public CourseService(CourseRepository courseRepository,
                         CourseImageRepository courseImageRepository,
                         FileStorageService fileStorageService) {
        this.courseRepository = courseRepository;
        this.courseImageRepository = courseImageRepository;
        this.fileStorageService = fileStorageService;
    }

    // ========== Публичные методы для пользовательской части (с JOIN FETCH) ==========

    public List<Course> getAllCourses() {
        return courseRepository.findAllWithImages().stream()
                .distinct()
                .collect(Collectors.toList());
    }

    public Course getCourseById(Long id) {
        return courseRepository.findByIdWithImages(id)
                .orElseThrow(() -> new IllegalArgumentException("Курс не найден"));
    }

    public List<Course> getCoursesByCategoryId(Long categoryId) {
        if (categoryId == null) {
            return Collections.emptyList();
        }
        return courseRepository.findByCategoryIdWithImages(categoryId).stream()
                .distinct()
                .collect(Collectors.toList());
    }

    public List<Course> searchCoursesByTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            return getAllCourses();
        }
        return courseRepository.findByTitleContainingIgnoreCaseWithImages(title.trim()).stream()
                .distinct()
                .collect(Collectors.toList());
    }

    // ========== Методы для админки и внутреннего использования ==========

    public List<Course> getByCategory(Category category) {
        if (category == null) {
            throw new IllegalArgumentException("Категория отсутствует");
        }
        return courseRepository.findByCategory(category);
    }

    @Transactional
    public Course createNewCourse(Course course) {
        validateCourse(course);
        return courseRepository.save(course);
    }

    @Transactional
    public Course updateCourse(Course updatedCourse, Long id) {
        Course existingCourse = getCourseById(id);

        if (updatedCourse.getTitle() != null && !updatedCourse.getTitle().trim().isEmpty()) {
            existingCourse.setTitle(updatedCourse.getTitle());
        }
        if (updatedCourse.getDescription() != null) {
            existingCourse.setDescription(updatedCourse.getDescription());
        }
        if (updatedCourse.getPrice() != null && updatedCourse.getPrice().compareTo(BigDecimal.ZERO) >= 0) {
            existingCourse.setPrice(updatedCourse.getPrice());
        }
        if (updatedCourse.getCategory() != null) {
            existingCourse.setCategory(updatedCourse.getCategory());
        }
        return courseRepository.save(existingCourse);
    }

    @Transactional
    public void deleteCourseById(Long id) {
        Course course = courseRepository.findByIdWithImages(id)
                .orElseThrow(() -> new IllegalArgumentException("Курс не найден"));
        List<String> imagePaths = course.getImages().stream()
                .map(CourseImage::getFilePath)
                .toList();

        courseRepository.delete(course);
        courseRepository.flush();

        for (String filePath : imagePaths) {
            try {
                fileStorageService.deleteFile(filePath);
                log.debug("Удалён файл: {}", filePath);
            } catch (IOException e) {
                log.error("Не удалось удалить файл: {}", filePath, e);
            }
        }
    }

    // ========== Управление изображениями ==========

    @Transactional
    public void addImagesToCourse(Long courseId, MultipartFile[] files) {
        if (files == null || files.length == 0) return;
        Course course = getCourseById(courseId);
        Hibernate.initialize(course.getImages());
        boolean hadImages = !course.getImages().isEmpty();
        int nextOrder = course.getImages().size();
        for (int i = 0; i < files.length; i++) {
            MultipartFile file = files[i];
            if (file.isEmpty()) continue;
            try {
                String path = fileStorageService.saveCourseImage(file);
                CourseImage img = new CourseImage();
                img.setFilePath(path);
                img.setCourse(course);
                img.setMain(!hadImages && i == 0);
                img.setSortOrder(nextOrder++);
                courseImageRepository.save(img);
                course.getImages().add(img);
            } catch (IOException e) {
                throw new RuntimeException("Ошибка сохранения изображения", e);
            }
        }
    }

    @Transactional
    public void deleteImage(Long imageId) {
        try {
            CourseImage img = courseImageRepository.findById(imageId)
                    .orElseThrow(() -> new IllegalArgumentException("Изображение не найдено"));
            Course course = img.getCourse();
            boolean wasMain = img.isMain();

            fileStorageService.deleteFile(img.getFilePath());
            courseImageRepository.delete(img);
            course.getImages().remove(img);
            entityManager.flush();

            if (wasMain && !course.getImages().isEmpty()) {
                List<CourseImage> remaining = courseImageRepository.findByCourseIdOrderBySortOrderAsc(course.getId());
                if (!remaining.isEmpty()) {
                    CourseImage newMain = remaining.get(0);
                    newMain.setMain(true);
                    courseImageRepository.save(newMain);
                    course.getImages().clear();
                    course.getImages().addAll(remaining);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Ошибка при удалении файла", e);
        }
    }

    @Transactional
    public void setMainImage(Long imageId) {
        CourseImage newMain = courseImageRepository.findById(imageId)
                .orElseThrow(() -> new IllegalArgumentException("Изображение не найдено"));
        Course course = newMain.getCourse();
        course.getImages().forEach(img -> img.setMain(false));
        newMain.setMain(true);
        courseImageRepository.saveAll(course.getImages());
    }

    private void validateCourse(Course course) {
        if (course == null || course.getTitle() == null || course.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Название курса не может быть пустым");
        }
        if (course.getPrice() == null || course.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Цена должна быть больше или равна нулю");
        }
        if (course.getCategory() == null || course.getCategory().getId() == null) {
            throw new IllegalArgumentException("Выберите категорию");
        }
    }
}