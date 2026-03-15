package com.example.diplomproject.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
public class UserDto {
    private Long id;
    private String username;
    private String email;
    private String role;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime registrationDate;
    private List<OrderDto> orderDtos;
    private List<CertificateDto> certificateDtos;
    private Long cartId;
    private List<ReviewDto> reviewDtos;
    private List<CourseAccessDto> courseAccessDtos;


}
