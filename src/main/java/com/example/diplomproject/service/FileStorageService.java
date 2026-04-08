package com.example.diplomproject.service;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path categoriesRoot = Paths.get("uploads/categories/");
    private final Path coursesRoot = Paths.get("uploads/courses/");

    @PostConstruct
    public void init() throws IOException {
        Files.createDirectories(categoriesRoot);
        Files.createDirectories(coursesRoot);
    }

    // ========== Сохранение нескольких изображений ==========
    public List<String> saveCategoryImages(MultipartFile[] files) throws IOException {
        List<String> savedPaths = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;
            String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path target = categoriesRoot.resolve(filename);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            savedPaths.add("/uploads/categories/" + filename);
        }
        return savedPaths;
    }

    public List<String> saveCourseImages(MultipartFile[] files) throws IOException {
        List<String> savedPaths = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;
            String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path target = coursesRoot.resolve(filename);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            savedPaths.add("/uploads/courses/" + filename);
        }
        return savedPaths;
    }

    // ========== Сохранение одного изображения ==========
    public String saveCategoryImage(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) return null;
        String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path target = categoriesRoot.resolve(filename);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        return "/uploads/categories/" + filename;
    }

    public String saveCourseImage(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) return null;
        String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path target = coursesRoot.resolve(filename);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        return "/uploads/courses/" + filename;
    }

    // ========== Удаление файла ==========
    public void deleteFile(String filePath) throws IOException {
        if (filePath == null || filePath.isEmpty()) return;
        // Убираем начальный слеш, если он есть
        String relativePath = filePath.startsWith("/") ? filePath.substring(1) : filePath;
        Path path = Paths.get(relativePath);
        Files.deleteIfExists(path);
    }
}