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
}