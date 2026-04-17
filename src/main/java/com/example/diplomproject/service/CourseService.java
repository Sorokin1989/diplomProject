package com.example.diplomproject.service;

import com.example.diplomproject.dto.CourseDto;
import com.example.diplomproject.entity.Category;
import com.example.diplomproject.entity.Course;
import com.example.diplomproject.entity.CourseImage;
import com.example.diplomproject.mapper.CourseMapper;
import com.example.diplomproject.repository.CourseImageRepository;
import com.example.diplomproject.repository.CourseRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CourseService {
    @PersistenceContext
    private EntityManager entityManager;
    private final CourseMapper courseMapper;

    private static final Logger log = LoggerFactory.getLogger(CourseService.class);

    private final CourseRepository courseRepository;
    private final CourseImageRepository courseImageRepository;
    private final FileStorageService fileStorageService;

    @Autowired
    public CourseService(CourseMapper courseMapper, CourseRepository courseRepository,
                         CourseImageRepository courseImageRepository,
                         FileStorageService fileStorageService) {
        this.courseMapper = courseMapper;
        this.courseRepository = courseRepository;
        this.courseImageRepository = courseImageRepository;
        this.fileStorageService = fileStorageService;
    }

    // ==================== ПОЛЬЗОВАТЕЛЬСКАЯ ЧАСТЬ (возвращаем DTO) ====================

    /**
     * Возвращает список всех доступных курсов в виде DTO.
     * Курсы с битыми ссылками на категории автоматически исключаются.
     */
    @Transactional(readOnly = true)
    public List<CourseDto> getAllCourses() {
        List<Course> courses = courseRepository.findAll();
        return courses.stream()
                .filter(this::isCategoryExists)
                .map(courseMapper::toCourseDto)
                .collect(Collectors.toList());
    }

    /**
     * Возвращает курс по ID в виде DTO.
     * Если категория битая, курс всё равно возвращается, но без информации о категории.
     */
    @Transactional(readOnly = true)
    public CourseDto getCourseDtoById(Long id) {
        Course course = courseRepository.findByIdWithImages(id)
                .orElseThrow(() -> new IllegalArgumentException("Курс не найден"));
        // Проверяем категорию, если битая – логируем, но курс возвращаем
        if (!isCategoryExists(course)) {
            log.warn("Курс {} имеет несуществующую категорию", id);
        }
        return courseMapper.toCourseDto(course);
    }

    /**
     * Возвращает список курсов по ID категории в виде DTO.
     */
    @Transactional(readOnly = true)
    public List<CourseDto> getCourseDtosByCategoryId(Long categoryId) {
        if (categoryId == null) {
            return Collections.emptyList();
        }
        List<Course> courses = courseRepository.findByCategoryIdWithImages(categoryId);
        return courses.stream()
                .filter(this::isCategoryExists)
                .map(courseMapper::toCourseDto)
                .collect(Collectors.toList());
    }

    /**
     * Поиск курсов по названию (частичное совпадение, без учёта регистра).
     */
    @Transactional(readOnly = true)
    public List<CourseDto> searchCourseDtosByTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            return getAllCourses();
        }
        List<Course> courses = courseRepository.findByTitleContainingIgnoreCaseWithImages(title.trim());
        return courses.stream()
                .filter(this::isCategoryExists)
                .map(courseMapper::toCourseDto)
                .collect(Collectors.toList());
    }

    // ==================== АДМИНСКАЯ ЧАСТЬ (возвращаем Entity) ====================

    /**
     * Для внутреннего использования и админки – возвращает сущность Course.
     * Не использовать в пользовательских контроллерах во избежание LazyInitializationException.
     */
    @Transactional(readOnly = true)
    public Course getCourseEntityById(Long id) {
        return courseRepository.findByIdWithImages(id)
                .orElseThrow(() -> new IllegalArgumentException("Курс не найден"));
    }

    /**
     * Список курсов для админки (сущности).
     * Не использовать в публичных шаблонах – только для редактирования.
     */
    @Transactional(readOnly = true)
    public List<Course> getAllCoursesForAdmin() {
        return courseRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Course> getCoursesByCategoryForAdmin(Category category) {
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
        Course existingCourse = getCourseEntityById(id);

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
        Course course = getCourseEntityById(id);
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

    // ==================== Управление изображениями (админка) ====================

    @Transactional
    public void addImagesToCourse(Long courseId, MultipartFile[] files) {
        if (files == null || files.length == 0) return;
        Course course = getCourseEntityById(courseId);
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

            Hibernate.initialize(course.getImages());
            boolean wasMain = img.isMain();

            fileStorageService.deleteFile(img.getFilePath());
            courseImageRepository.delete(img);
            course.getImages().remove(img);

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
        Hibernate.initialize(course.getImages());
        course.getImages().forEach(img -> img.setMain(false));
        newMain.setMain(true);
        courseImageRepository.saveAll(course.getImages());
    }

    // ==================== Вспомогательные методы ====================

    private boolean isCategoryExists(Course course) {
        try {
            course.getCategory().getId();
            return true;
        } catch (EntityNotFoundException e) {
            log.warn("Курс {} имеет несуществующую категорию, пропускаем", course.getId());
            return false;
        }
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

    @Transactional
    public void uploadCourseMaterials(Long courseId, MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Файл не выбран");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".zip")) {
            throw new IllegalArgumentException("Допустимы только ZIP-архивы");
        }
        // 1. Находим курс
        Course course = getCourseEntityById(courseId);

        // 2. Сохраняем новый файл на диск
        String newFilePath = saveMaterialsFile(file, courseId);

        // 3. Запоминаем старый путь (если был)
        String oldFilePath = course.getMaterialsPath();

        // 4. Обновляем путь в БД
        course.setMaterialsPath(newFilePath);
        courseRepository.save(course);

        // 5. Удаляем старый файл после успешного обновления БД
        if (oldFilePath != null && !oldFilePath.isEmpty()) {
            try {
                Path oldPath = Paths.get(oldFilePath).normalize();
                Files.deleteIfExists(oldPath);
                log.info("Старый файл материалов удалён: {}", oldFilePath);
            } catch (IOException e) {
                log.warn("Не удалось удалить старый файл: {}", oldFilePath, e);
                // Не прерываем выполнение, так как новый файл уже загружен
            }
        }
    }

    // Вспомогательный метод – сохранение файла на диск
    private String saveMaterialsFile(MultipartFile file, Long courseId) {
        try {
            String uploadDir = "uploads/materials/";
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            String fileName = String.format("course_%d_%d.zip", courseId, System.currentTimeMillis());
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            return uploadDir + fileName; // относительный путь для БД
        } catch (IOException e) {
            throw new RuntimeException("Ошибка сохранения файла материалов", e);
        }
    }

    @Transactional
    public void deleteCourseMaterials(Long courseId) {
        Course course = getCourseEntityById(courseId);
        String materialsPath = course.getMaterialsPath();
        if (materialsPath != null && !materialsPath.isEmpty()) {
            try {
                Path filePath = Paths.get(materialsPath).normalize();
                Files.deleteIfExists(filePath);
                log.info("Файл материалов удалён: {}", materialsPath);
            } catch (IOException e) {
                log.warn("Не удалось удалить файл материалов: {}", materialsPath, e);
            }
            course.setMaterialsPath(null);
            courseRepository.save(course);
        } else {
            throw new IllegalArgumentException("Материалы не найдены");
        }
    }
}