package com.example.diplomproject.service;

import com.example.diplomproject.entity.Category;
import com.example.diplomproject.entity.CategoryImage;
import com.example.diplomproject.repository.CategoryImageRepository;
import com.example.diplomproject.repository.CategoryRepository;
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
import java.util.List;
import java.util.NoSuchElementException;


@Service
public class CategoryService {

    private static final Logger log = LoggerFactory.getLogger(CategoryService.class);

    @PersistenceContext
    private EntityManager entityManager;

    private final CategoryRepository categoryRepository;
    private final CategoryImageRepository categoryImageRepository;
    private final FileStorageService fileStorageService;

    @Autowired
    public CategoryService(CategoryRepository categoryRepository,
                           CategoryImageRepository categoryImageRepository,
                           FileStorageService fileStorageService) {
        this.categoryRepository = categoryRepository;
        this.categoryImageRepository = categoryImageRepository;
        this.fileStorageService = fileStorageService;
    }

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    public Category getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Категория не найдена"));
        // Принудительно загружаем изображения, чтобы избежать LazyInitializationException
        Hibernate.initialize(category.getImages());
        return category;
    }

    @Transactional
    public Category addNewCategory(Category category) {
        if (category.getTitle() == null || category.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Название категории не может быть пустым");
        }
        if (categoryRepository.existsByTitleContainingIgnoreCase(category.getTitle())) {
            throw new IllegalArgumentException("Такая категория уже существует");
        }
        return categoryRepository.save(category);
    }

    @Transactional
    public Category updateCategory(Long id, Category updatedCategory) {
        if (updatedCategory == null) {
            throw new IllegalArgumentException("Объект категории не может быть null");
        }
        Category existingCategory = getCategoryById(id);
        if (updatedCategory.getTitle() == null || updatedCategory.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Название категории не может быть пустым");
        }
        if (!existingCategory.getTitle().equals(updatedCategory.getTitle())
                && categoryRepository.existsByTitleIgnoreCase(updatedCategory.getTitle())) {
            throw new IllegalArgumentException("Категория с таким названием уже есть");
        }
        existingCategory.setTitle(updatedCategory.getTitle());
        existingCategory.setDescription(updatedCategory.getDescription());
        // Старое поле imageUrl не используется – изображения управляются отдельно
        return categoryRepository.save(existingCategory);
    }

    @Transactional
    public void deleteCategoryById(Long id) {
        Category category = getCategoryById(id);
        List<String> imagePaths = category.getImages().stream()
                .map(CategoryImage::getFilePath)
                .toList();

        categoryRepository.delete(category);
        categoryRepository.flush();  // принудительный сброс, чтобы поймать исключение

        for (String path : imagePaths) {
            try {
                fileStorageService.deleteFile(path);
            } catch (IOException e) {
                log.error("Не удалось удалить файл: {}", path, e);
            }
        }
    }

    // ========== Управление изображениями ==========

    @Transactional
    public void addImagesToCategory(Long categoryId, MultipartFile[] files) {
        if (files == null || files.length == 0) return;
        Category category = getCategoryById(categoryId);
        // Коллекция уже инициализирована в getCategoryById, но на всякий случай:
        Hibernate.initialize(category.getImages());
        boolean hadImages = !category.getImages().isEmpty();
        int nextOrder = category.getImages().size();
        for (int i = 0; i < files.length; i++) {
            MultipartFile file = files[i];
            if (file.isEmpty()) continue;
            try {
                String path = fileStorageService.saveCategoryImage(file);
                CategoryImage img = new CategoryImage();
                img.setFilePath(path);
                img.setCategory(category);
                img.setMain(!hadImages && i == 0);
                img.setSortOrder(nextOrder++);
                categoryImageRepository.save(img);
                category.getImages().add(img);
            } catch (IOException e) {
                throw new RuntimeException("Ошибка сохранения изображения", e);
            }
        }
    }

    @Transactional
    public void deleteImage(Long imageId) {
        try {
            CategoryImage img = categoryImageRepository.findById(imageId)
                    .orElseThrow(() -> new NoSuchElementException("Изображение не найдено"));
            Category category = img.getCategory();
            boolean wasMain = img.isMain();

            fileStorageService.deleteFile(img.getFilePath());
            categoryImageRepository.delete(img);
            category.getImages().remove(img);
            entityManager.flush();

            if (wasMain && !category.getImages().isEmpty()) {
                List<CategoryImage> remaining = categoryImageRepository.findByCategoryIdOrderBySortOrderAsc(category.getId());
                if (!remaining.isEmpty()) {
                    CategoryImage newMain = remaining.get(0);
                    newMain.setMain(true);
                    categoryImageRepository.save(newMain);
                    category.getImages().clear();
                    category.getImages().addAll(remaining);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Ошибка при удалении файла", e);
        }
    }

    @Transactional
    public void setMainImage(Long imageId) {
        CategoryImage newMain = categoryImageRepository.findById(imageId)
                .orElseThrow(() -> new NoSuchElementException("Изображение не найдено"));
        Category category = newMain.getCategory();
        category.getImages().forEach(img -> img.setMain(false));
        newMain.setMain(true);
        categoryImageRepository.saveAll(category.getImages());
    }
}