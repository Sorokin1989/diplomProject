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
            String path = saveCategoryImage(file);
            if (path != null) savedPaths.add(path);
        }
        return savedPaths;
    }

    public List<String> saveCourseImages(MultipartFile[] files) throws IOException {
        List<String> savedPaths = new ArrayList<>();
        for (MultipartFile file : files) {
            String path = saveCourseImage(file);
            if (path != null) savedPaths.add(path);
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

        Path root = null;
        if (filePath.startsWith("/uploads/categories/")) {
            root = categoriesRoot;
        } else if (filePath.startsWith("/uploads/courses/")) {
            root = coursesRoot;
        }

        if (root != null) {
            String fileName = filePath.substring(filePath.lastIndexOf('/') + 1);
            Path file = root.resolve(fileName);
            Files.deleteIfExists(file);
        } else {
            // fallback для совместимости (если путь не соответствует ожидаемым префиксам)
            String relativePath = filePath.startsWith("/") ? filePath.substring(1) : filePath;
            Path path = Paths.get(relativePath);
            Files.deleteIfExists(path);
        }
    }
}