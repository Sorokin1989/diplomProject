package com.example.diplomproject.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class FileStorageServiceTest {

    @TempDir
    Path tempDir;

    private FileStorageService fileStorageService;

    @BeforeEach
    void setUp() throws Exception {
        fileStorageService = new FileStorageService();
        // Перенаправляем корневые пути на временную директорию
        var categoriesField = FileStorageService.class.getDeclaredField("categoriesRoot");
        categoriesField.setAccessible(true);
        categoriesField.set(fileStorageService, tempDir.resolve("uploads/categories"));

        var coursesField = FileStorageService.class.getDeclaredField("coursesRoot");
        coursesField.setAccessible(true);
        coursesField.set(fileStorageService, tempDir.resolve("uploads/courses"));

        // Инициализируем директории (вызываем @PostConstruct)
        fileStorageService.init();
    }

    @Test
    void saveCategoryImage_shouldSaveFileAndReturnPath() throws IOException {
        MultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "content".getBytes());
        String savedPath = fileStorageService.saveCategoryImage(file);

        assertThat(savedPath).startsWith("/uploads/categories/");
        Path expectedFile = tempDir.resolve("uploads/categories").resolve(savedPath.substring("/uploads/categories/".length()));
        assertThat(Files.exists(expectedFile)).isTrue();
    }

    @Test
    void saveCategoryImages_shouldSaveMultipleFiles() throws IOException {
        MultipartFile file1 = new MockMultipartFile("file1", "a.jpg", "image/jpeg", "content".getBytes());
        MultipartFile file2 = new MockMultipartFile("file2", "b.jpg", "image/jpeg", "content".getBytes());
        List<String> paths = fileStorageService.saveCategoryImages(new MultipartFile[]{file1, file2});

        assertThat(paths).hasSize(2);
        for (String path : paths) {
            Path expectedFile = tempDir.resolve("uploads/categories").resolve(path.substring("/uploads/categories/".length()));
            assertThat(Files.exists(expectedFile)).isTrue();
        }
    }

    @Test
    void deleteFile_shouldRemoveFile() throws IOException {
        MultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "content".getBytes());
        String path = fileStorageService.saveCategoryImage(file);
        assertThat(Files.exists(tempDir.resolve("uploads/categories").resolve(path.substring("/uploads/categories/".length())))).isTrue();

        fileStorageService.deleteFile(path);
        Path expectedFile = tempDir.resolve("uploads/categories").resolve(path.substring("/uploads/categories/".length()));
        assertThat(Files.exists(expectedFile)).isFalse();
    }

    @Test
    void deleteFile_shouldNotThrowIfFileNotExists() throws IOException {
        // просто не должно быть исключения
        fileStorageService.deleteFile("/uploads/categories/nonexistent.jpg");
    }
}