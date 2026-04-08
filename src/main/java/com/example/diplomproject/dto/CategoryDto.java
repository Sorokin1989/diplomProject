package com.example.diplomproject.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class CategoryDto {
    private Long id;
    private String title;
    private String description;

    // Для обратной совместимости – главное изображение (можно вычислить из списка)
    private String imageUrl;

    // Список URL всех изображений
    private List<String> imageUrls = new ArrayList<>();

    private List<CourseDto> courseDtos;

    // Вспомогательный метод для установки главного изображения из списка
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getImageUrl() {
        if (imageUrl == null && !imageUrls.isEmpty()) {
            // По умолчанию первое изображение считаем главным
            return imageUrls.get(0);
        }
        return imageUrl;
    }
}