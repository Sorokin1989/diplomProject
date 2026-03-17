package com.example.diplomproject.mapper;

import com.example.diplomproject.dto.CourseAccessDto;
import com.example.diplomproject.entity.CourseAccess;
import org.springframework.stereotype.Component;

@Component
public class CourseAccessMapper {

    public CourseAccessDto toCourseAccessDto(CourseAccess courseAccess) {
        if (courseAccess == null) {
            return null;
        }

        CourseAccessDto courseAccessDTO = new CourseAccessDto();
        courseAccessDTO.setId(courseAccess.getId());

        if (courseAccess.getUser() != null) {
            courseAccessDTO.setUserId(courseAccess.getUser().getId());
            courseAccessDTO.setUsername(courseAccess.getUser().getUsername());
        }

        if (courseAccess.getCourse() != null) {
            courseAccessDTO.setCourseId(courseAccess.getCourse().getId());
            courseAccessDTO.setCourseTitle(courseAccess.getCourse().getTitle());
        }

        if (courseAccess.getOrder() != null) {
            courseAccessDTO.setOrderId(courseAccess.getOrder().getId());
        }

        courseAccessDTO.setGrantedAt(courseAccess.getGrantedAt());
        courseAccessDTO.setExpiresAt(courseAccess.getExpiresAt());

        courseAccessDTO.setCreatedAt(courseAccess.getCreatedAt());
        courseAccessDTO.setActive(courseAccess.isActive());

        return courseAccessDTO;
    }
}
