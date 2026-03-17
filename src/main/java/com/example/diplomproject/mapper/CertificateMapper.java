package com.example.diplomproject.mapper;

import com.example.diplomproject.dto.CertificateDto;
import com.example.diplomproject.entity.Certificate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class CertificateMapper {

    public CertificateDto toCertificateDto(Certificate certificate) {
        if (certificate == null)
            return null;

        CertificateDto certificateDto = new CertificateDto();

        certificateDto.setId(certificate.getId());

        if (certificate.getUser() != null) {
            certificateDto.setUserId(certificate.getUser().getId());
            certificateDto.setUsername(certificate.getUser().getUsername());
        }

        if (certificate.getCourse() != null) {
            certificateDto.setCourseId(certificate.getCourse().getId());
            certificateDto.setCourseTitle(certificate.getCourse().getTitle());
        }

        certificateDto.setCreatedAt(certificate.getCreatedAt() != null ? certificate.getCreatedAt() : LocalDateTime.now());

        certificateDto.setCertificateId(certificate.getCertificateId());
        certificateDto.setCertificateUrl(certificate.getCertificateUrl());

        return certificateDto;
    }

}
