package com.example.diplomproject.mapper;

import com.example.diplomproject.dto.CourseDto;
import com.example.diplomproject.entity.Course;
import org.springframework.stereotype.Component;

@Component
public class CourseMapper {

    public CourseDto toCourseDto(Course course) {

        if (course == null){
            return null;
        }
        CourseDto courseDto = new CourseDto();
        courseDto.setId(course.getId());
        courseDto.setTitle(course.getTitle());
        courseDto.setDescription(course.getDescription());
        courseDto.setPrice(course.getPrice());
        courseDto.setAuthor(course.getAuthor());

        if(course.getCategory() !=null){
        courseDto.setCategoryTitle(course.getCategory().getTitle());
        }
        courseDto.setImageUrl(course.getImageUrl());
        courseDto.setCreatedAt(course.getCreatedAt());
        courseDto.setReviewCount(course.getReviewCount());

        return courseDto;

    }
}
