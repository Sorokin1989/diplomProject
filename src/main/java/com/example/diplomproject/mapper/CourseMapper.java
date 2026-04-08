package com.example.diplomproject.mapper;

import com.example.diplomproject.dto.CourseDto;
import com.example.diplomproject.entity.Course;
import org.springframework.stereotype.Component;

@Component
public class CourseMapper {

    public CourseDto toCourseDto(Course course) {
        if (course == null) {
            return null;
        }
        CourseDto courseDto = new CourseDto();
        courseDto.setId(course.getId());
        courseDto.setTitle(course.getTitle());
        courseDto.setDescription(course.getDescription());
        courseDto.setPrice(course.getPrice());
        courseDto.setAuthor(course.getAuthor());

        if (course.getCategory() != null) {
            courseDto.setCategoryId(course.getCategory().getId());
        }
        // Получаем главное изображение из списка images
        courseDto.setImageUrl(course.getMainImageUrl());

        // Если нужно передать все изображения, раскомментировать:
        // List<String> urls = course.getImages().stream()
        //         .sorted(Comparator.comparingInt(CourseImage::getSortOrder))
        //         .map(CourseImage::getFilePath)
        //         .collect(Collectors.toList());
        // courseDto.setImageUrls(urls);

        courseDto.setCreatedAt(course.getCreatedAt());
        courseDto.setReviewCount(course.getReviewCount());
        return courseDto;
    }

    public Course fromCourseDtoToEntity(CourseDto courseDto) {
        if (courseDto == null) return null;

        Course course = new Course();
        course.setId(courseDto.getId());
        course.setTitle(courseDto.getTitle());
        course.setDescription(courseDto.getDescription());
        course.setPrice(courseDto.getPrice());
        course.setAuthor(courseDto.getAuthor());
        // Поле imageUrl больше не существует, не устанавливаем
        // course.setImageUrl(courseDto.getImageUrl());
        course.setCreatedAt(courseDto.getCreatedAt());
        course.setReviewCount(courseDto.getReviewCount());
        // Категория, изображения и другие связи должны устанавливаться отдельно в сервисе
        return course;
    }
}