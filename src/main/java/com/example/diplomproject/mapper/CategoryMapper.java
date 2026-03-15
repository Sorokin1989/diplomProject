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
        categoryDto.setImageUrl(category.getImageUrl());

        if (category.getCourses() != null) {
            List<CourseDto> courseDtos = category.getCourses().stream().map(courseMapper::toCourseDto).collect(Collectors.toList());
            categoryDto.setCourseDtos(courseDtos);
        }
        return categoryDto;
    }
}
