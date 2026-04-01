package com.example.diplomproject.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class CertificateDto {
    private Long id;
    private Long userId;
    private String username;
    private Long courseId;
    private String courseTitle;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    private String certificateId;
    private String certificateUrl;

    private boolean revoked;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime revokedDate;

    // Конструктор для проекции
    public CertificateDto(Long id, Long userId, String username, Long courseId, String courseTitle,
                          LocalDateTime createdAt, String certificateId, String certificateUrl,
                          boolean revoked, LocalDateTime revokedDate) {
        this.id = id;
        this.userId = userId;
        this.username = username;
        this.courseId = courseId;
        this.courseTitle = courseTitle;
        this.createdAt = createdAt;
        this.certificateId = certificateId;
        this.certificateUrl = certificateUrl;
        this.revoked = revoked;
        this.revokedDate = revokedDate;
    }
}