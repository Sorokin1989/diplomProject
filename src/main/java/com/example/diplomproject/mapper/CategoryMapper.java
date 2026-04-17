package com.example.diplomproject.mapper;

import com.example.diplomproject.dto.CategoryDto;
import com.example.diplomproject.dto.CourseDto;
import com.example.diplomproject.entity.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class CategoryMapper {

    private final CourseMapper courseMapper;

    @Autowired
    public CategoryMapper(CourseMapper courseMapper) {
        this.courseMapper = courseMapper;
    }

    public CategoryDto toCategoryDTO(Category category) {
        if (category == null) {
            return null;
        }
        CategoryDto categoryDto = new CategoryDto();

        categoryDto.setId(category.getId());
        categoryDto.setTitle(category.getTitle());
        categoryDto.setDescription(category.getDescription());
        // Передаём URL главного изображения (для обратной совместимости с DTO)
        categoryDto.setImageUrl(category.getMainImageUrl());

        if (category.getCourses() != null) {
            List<CourseDto> courseDtos = category.getCourses().stream()
                    .map(courseMapper::toCourseDto)
                    .collect(Collectors.toList());
            categoryDto.setCourseDtos(courseDtos);
        }
        return categoryDto;
    }

//    public Category fromCategoryDtoToEntity(CategoryDto categoryDto) {
//        if (categoryDto == null) return null;
//
//        Category category = new Category();
//
//        category.setId(categoryDto.getId());
//        category.setTitle(categoryDto.getTitle());
//        category.setDescription(categoryDto.getDescription());
//        // Поле imageUrl больше не устанавливается – изображения управляются через CategoryImageService
//
//        if (categoryDto.getCourseDtos() != null) {
//            category.setCourses(categoryDto.getCourseDtos().stream()
//                    .map(courseMapper::fromCourseDtoToEntity)
//                    .toList());
//        }
//        return category;
//    }
}