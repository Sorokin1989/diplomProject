package com.example.diplomproject.mapper;

import com.example.diplomproject.dto.DiscountDto;
import com.example.diplomproject.entity.Discount;
import com.example.diplomproject.enums.DiscountType;
import org.springframework.stereotype.Component;

@Component
public class DiscountMapper {

    public DiscountDto toDiscountDto(Discount discount) {

        if (discount == null) {
            return null;
        }

        DiscountDto discountDto = new DiscountDto();

        discountDto.setId(discount.getId());
        discountDto.setTitle(discount.getTitle());
        discountDto.setDescription(discount.getDescription());
        discountDto.setDiscountType(String.valueOf(discount.getDiscountType()));
        discountDto.setDiscountValue(discount.getDiscountValue());
        discountDto.setStartDate(discount.getStartDate());
        discountDto.setEndDate(discount.getEndDate());

        if (discount.getApplicableCourses() != null) {
            discountDto.setApplicableCourseIds(discount.getApplicableCourses().stream().
                    map(course -> course.getId()).toList());
            discountDto.setApplicableCourseTitles(discount.getApplicableCourses().stream().
                    map(course -> course.getTitle()).toList());
        }

        if (discount.getApplicableCategories() != null) {
            discountDto.setApplicableCategoryIds(discount.getApplicableCategories().stream().
                    map(category -> category.getId()).toList());
            discountDto.setApplicableCategoryTitles(discount.getApplicableCategories().stream().
                    map(category -> category.getTitle()).toList());
        }

        discountDto.setMinOrderAmount(discount.getMinOrderAmount());
        discountDto.setActive(discount.isActive());
        discountDto.setCreatedAt(discount.getCreatedAt());

        return discountDto;
    }

    public Discount fromDiscountToDToEntity(DiscountDto discountDto) {
        if (discountDto == null) {
            return null;
        }

        Discount discount = new Discount();
        discount.setId(discountDto.getId());
        discount.setTitle(discountDto.getTitle());
        discount.setDescription(discountDto.getDescription());
        discount.setDiscountType(DiscountType.valueOf(discountDto.getDiscountType()));
        discount.setDiscountValue(discountDto.getDiscountValue());
        discount.setStartDate(discountDto.getStartDate());
        discount.setEndDate((discountDto.getEndDate()));
        discount.setMinOrderAmount(discountDto.getMinOrderAmount());
        discount.setActive(discount.isActive());
        discount.setCreatedAt(discountDto.getCreatedAt());

        return discount;


    }
}
