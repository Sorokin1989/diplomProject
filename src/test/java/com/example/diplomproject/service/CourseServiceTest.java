package com.example.diplomproject.service;

import com.example.diplomproject.dto.CourseDto;
import com.example.diplomproject.entity.Category;
import com.example.diplomproject.entity.Course;
import com.example.diplomproject.entity.CourseImage;
import com.example.diplomproject.mapper.CourseMapper;
import com.example.diplomproject.repository.CourseImageRepository;
import com.example.diplomproject.repository.CourseRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;
    @Mock
    private CourseImageRepository courseImageRepository;
    @Mock
    private FileStorageService fileStorageService;
    @Mock
    private CourseMapper courseMapper;
    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private CourseService courseService;

    private Course course;
    private Category category;
    private CourseImage courseImage;
    private CourseDto courseDto;

    @BeforeEach
    void setUp() {
        category = new Category();
        category.setId(1L);

        course = new Course();
        course.setId(10L);
        course.setTitle("Test Course");
        course.setDescription("Description");
        course.setPrice(BigDecimal.valueOf(99.99));
        course.setCategory(category);
        course.setActive(true);

        courseImage = new CourseImage();
        courseImage.setId(100L);
        courseImage.setFilePath("/uploads/courses/image.jpg");
        courseImage.setMain(true);
        courseImage.setCourse(course);
        course.setImages(List.of(courseImage));

        courseDto = new CourseDto();
        courseDto.setId(10L);
        courseDto.setTitle("Test Course");
    }

    // ========== getAllCourses ==========
    @Test
    void getAllCourses_shouldReturnFilteredCourses() {
        when(courseRepository.findAll()).thenReturn(List.of(course));
        when(courseMapper.toCourseDto(course)).thenReturn(courseDto);

        List<CourseDto> result = courseService.getAllCourses();

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(courseDto);
        verify(courseMapper).toCourseDto(course);
    }

    // ========== getCourseDtoById ==========
    @Test
    void getCourseDtoById_shouldReturnDto() {
        when(courseRepository.findByIdWithImages(10L)).thenReturn(Optional.of(course));
        when(courseMapper.toCourseDto(course)).thenReturn(courseDto);

        CourseDto result = courseService.getCourseDtoById(10L);

        assertThat(result).isEqualTo(courseDto);
    }

    @Test
    void getCourseDtoById_shouldThrowWhenNotFound() {
        when(courseRepository.findByIdWithImages(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> courseService.getCourseDtoById(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Курс не найден");
    }

    // ========== createNewCourse ==========
    @Test
    void createNewCourse_shouldSaveValidCourse() {
        when(courseRepository.save(any(Course.class))).thenReturn(course);
        Course saved = courseService.createNewCourse(course);
        assertThat(saved).isEqualTo(course);
        verify(courseRepository).save(course);
    }

    @Test
    void createNewCourse_shouldThrowWhenTitleEmpty() {
        course.setTitle("");
        assertThatThrownBy(() -> courseService.createNewCourse(course))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Название курса не может быть пустым");
        verify(courseRepository, never()).save(any());
    }

    // ========== updateCourse ==========
    @Test
    void updateCourse_shouldUpdateFields() {
        Course updatedData = new Course();
        updatedData.setTitle("New Title");
        updatedData.setDescription("New Desc");
        updatedData.setPrice(BigDecimal.valueOf(49.99));
        updatedData.setCategory(category);

        when(courseRepository.findByIdWithImages(10L)).thenReturn(Optional.of(course));
        when(courseRepository.save(any(Course.class))).thenReturn(course);

        Course result = courseService.updateCourse(updatedData, 10L);

        assertThat(result.getTitle()).isEqualTo("New Title");
        assertThat(result.getDescription()).isEqualTo("New Desc");
        assertThat(result.getPrice()).isEqualByComparingTo("49.99");
        verify(courseRepository).save(course);
    }

    // ========== deleteCourseById ==========
    @Test
    void deleteCourseById_shouldDeleteCourseAndFiles() throws IOException {
        when(courseRepository.findByIdWithImages(10L)).thenReturn(Optional.of(course));
        doNothing().when(fileStorageService).deleteFile(anyString());

        courseService.deleteCourseById(10L);

        verify(courseRepository).delete(course);
        verify(fileStorageService).deleteFile("/uploads/courses/image.jpg");
    }

    // ========== addImagesToCourse ==========
    @Test
    void addImagesToCourse_shouldAddImages() throws IOException {
        MultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "content".getBytes());
        MultipartFile[] files = {file};
        when(courseRepository.findByIdWithImages(10L)).thenReturn(Optional.of(course));
        when(fileStorageService.saveCourseImage(any())).thenReturn("/uploads/courses/new.jpg");

        // Убедимся, что коллекция изменяемая
        List<CourseImage> images = new ArrayList<>();
        images.add(courseImage); // было одно изображение
        course.setImages(images); // теперь список изменяемый

        courseService.addImagesToCourse(10L, files);

        verify(courseImageRepository).save(any(CourseImage.class));
        assertThat(course.getImages()).hasSize(2);
    }

    // ========== setMainImage ==========
    @Test
    void setMainImage_shouldUpdateMainFlag() {
        // Создаём второе изображение
        CourseImage newMain = new CourseImage();
        newMain.setId(20L);
        newMain.setMain(false);
        newMain.setCourse(course);

        // ✅ Создаём изменяемый список с двумя изображениями
        List<CourseImage> images = new ArrayList<>();
        images.add(courseImage);
        images.add(newMain);
        course.setImages(images);

        when(courseImageRepository.findById(20L)).thenReturn(Optional.of(newMain));
        when(courseImageRepository.saveAll(anyList())).thenReturn(List.of());

        courseService.setMainImage(20L);

        assertThat(newMain.isMain()).isTrue();
        assertThat(courseImage.isMain()).isFalse();
        verify(courseImageRepository).saveAll(anyList());
    }

    // ========== deleteImage ==========
    @Test
    void deleteImage_shouldDeleteImageAndUpdateMain() throws IOException {
        // убедимся, что коллекция изменяема
        List<CourseImage> images = new ArrayList<>(course.getImages());
        course.setImages(images);

        when(courseImageRepository.findById(100L)).thenReturn(Optional.of(courseImage));

        courseService.deleteImage(100L);

        verify(fileStorageService).deleteFile("/uploads/courses/image.jpg");
        verify(courseImageRepository).delete(courseImage);
    }

    // ========== uploadCourseMaterials ==========
    @Test
    void uploadCourseMaterials_shouldSaveNewFileAndDeleteOld() throws IOException {
        MultipartFile file = new MockMultipartFile("file", "materials.zip", "application/zip", "content".getBytes());
        when(courseRepository.findByIdWithImages(10L)).thenReturn(Optional.of(course));

        // ✅ Устанавливаем старый путь, чтобы удаление было вызвано
        course.setMaterialsPath("uploads/materials/old_course.zip");

        try (var mockedFiles = mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.createDirectories(any(Path.class))).thenReturn(Path.of(""));
            mockedFiles.when(() -> Files.copy(any(InputStream.class), any(Path.class), any(StandardCopyOption[].class)))
                    .thenReturn(1L);
            mockedFiles.when(() -> Files.deleteIfExists(any(Path.class))).thenReturn(true);

            courseService.uploadCourseMaterials(10L, file);

            assertThat(course.getMaterialsPath()).isNotNull();
            verify(courseRepository).save(course);
            // Проверяем, что удаление старого файла вызвано один раз
            mockedFiles.verify(() -> Files.deleteIfExists(any(Path.class)), times(1));
        }
    }

    // ========== deleteCourseMaterials ==========
    @Test
    void deleteCourseMaterials_shouldDeleteFileAndNullPath() throws IOException {
        course.setMaterialsPath("uploads/materials/course_10_12345.zip");
        when(courseRepository.findByIdWithImages(10L)).thenReturn(Optional.of(course));
        try (var mockedFiles = mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.deleteIfExists(any())).thenReturn(true);
            courseService.deleteCourseMaterials(10L);
            assertThat(course.getMaterialsPath()).isNull();
            verify(courseRepository).save(course);
            mockedFiles.verify(() -> Files.deleteIfExists(any()), times(1));
        }
    }
}