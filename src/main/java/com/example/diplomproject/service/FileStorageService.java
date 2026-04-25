package com.example.diplomproject.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${app.upload.path}")
    private String uploadPath;

    private Path categoriesRoot;
    private Path coursesRoot;
    private Path materialsRoot;
    private Path certificatesRoot;

    @PostConstruct
    public void init() throws IOException {
        Path base = Paths.get(uploadPath).normalize();
        categoriesRoot = base.resolve("categories");
        coursesRoot = base.resolve("courses");
        materialsRoot = base.resolve("materials");
        certificatesRoot = base.resolve("certificates");

        Files.createDirectories(categoriesRoot);
        Files.createDirectories(coursesRoot);
        Files.createDirectories(materialsRoot);
        Files.createDirectories(certificatesRoot);
    }

    public String saveCategoryImage(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) return null;
        String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path target = categoriesRoot.resolve(filename);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        return "/uploads/categories/" + filename; // относительный путь без uploadPath
    }

    public String saveCourseImage(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) return null;
        String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path target = coursesRoot.resolve(filename);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        return "/uploads/courses/" + filename;
    }

    public String saveMaterialsFile(MultipartFile file, String subPath) throws IOException {
        // Например, subPath = "materials/" + fileName
        Path target = materialsRoot.resolve(subPath.substring("materials/".length()));
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        return subPath;
    }

    public void deleteFile(String relativePath) throws IOException {
        if (relativePath == null || relativePath.isEmpty()) return;
        // Убираем ведущий слеш и возможный префикс "uploads/" или сам uploadPath
        String cleanPath = relativePath.replaceFirst("^/?(uploads/)?", "");
        Path base = Paths.get(uploadPath).normalize();
        Path filePath = base.resolve(cleanPath).normalize();
        if (!filePath.startsWith(base)) {
            throw new SecurityException("Попытка доступа за пределы базовой папки: " + cleanPath);
        }
        Files.deleteIfExists(filePath);
    }
}