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
        certificateDto.setRevoked(certificate.isRevoked());
        certificateDto.setRevokedDate(certificate.getRevokedDate()); // или getRevokedDate, смотрите по вашей сущности

        return certificateDto;
    }
    public Certificate fromCertificateDtoToEntity(CertificateDto certificateDto){

        if (certificateDto==null)return null;

        Certificate certificate=new Certificate();
        certificate.setId(certificateDto.getId());
        certificate.setCreatedAt(certificateDto.getCreatedAt());
        certificate.setCertificateId(certificateDto.getCertificateId());
        certificate.setCertificateUrl(certificateDto.getCertificateUrl());
        certificate.setRevoked(certificateDto.isRevoked());
        certificate.setRevokedDate(certificateDto.getRevokedDate());

        return certificate;

    }

}
