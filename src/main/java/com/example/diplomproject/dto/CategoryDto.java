package com.example.diplomproject.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
public class CategoryDto {
    private Long id;
    private String title;
    private String description;
    private String imageUrl;
    private List<CourseDto> courseDtos;
}
