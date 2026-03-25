package com.example.diplomproject.service;

import com.example.diplomproject.entity.Category;
import com.example.diplomproject.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Autowired
    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    /**
     * Получение всех категорий
     */

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }


    /**
     * Получение категории по ID
     */
    public Category getCategoryById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Категория не найдена"));

    }

    /**
     * Создание новой категории
     */
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
                && categoryRepository.existsByTitleContainingIgnoreCase(updatedCategory.
                getTitle())) {
            throw new IllegalArgumentException("Категория с таким названием уже есть");
        }
        existingCategory.setTitle(updatedCategory.getTitle());
        existingCategory.setDescription(updatedCategory.getDescription());
        return categoryRepository.save(existingCategory);
    }

    /**
     * Удаление категории по ID
     */
    @Transactional
    public void deleteCategoryByID(Long id) {
        Category category = getCategoryById(id);
        categoryRepository.delete(category);

    }

    /**
     * Проверка существования категории по имени
     */

    public boolean existsByTitle(String title) {
        return categoryRepository.existsByTitle(title);
    }

}



