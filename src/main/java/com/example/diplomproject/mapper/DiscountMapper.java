package com.example.diplomproject.mapper;

import com.example.diplomproject.dto.DiscountDto;
import com.example.diplomproject.entity.Discount;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DiscountMapper {

    public DiscountDto toDiscountDto(Discount discount) {

        if (discount == null)
            return null;

        DiscountDto discountDto=new DiscountDto();

        discountDto.setId(discount.getId());
        discountDto.setTitle(discount.getTitle());
        discountDto.setDescription(discount.getDescription());
        discountDto.setDiscountType(String.valueOf(discount.getDiscountType()));
        discountDto.setDiscountValue(discount.getDiscountValue());
        discountDto.setStartDate(discount.getStartDate());
        discountDto.setEndDate(discount.getEndDate());

        if (discount.getApplicableCourses() !=null){
            discountDto.setApplicableCourseIds(discount.getApplicableCourses().stream().map(course -> course.getId()).toList());
            discountDto.setApplicableCourseTitles(discount.getApplicableCourses().stream().map(course->course.getTitle()).toList());
        }

        if(discount.getApplicableCategories()!=null ){
            discountDto.setApplicableCategoryIds(discount.getApplicableCategories().stream().map(category -> category.getId()).toList());
            discountDto.setApplicableCategoryTitles(discount.getApplicableCategories().stream().map(category -> category.getTitle()).toList());
        }

        discountDto.setMinOrderAmount(discount.getMinOrderAmount());
        discountDto.setActive(discount.isActive());
        discountDto.setCreatedAt(discount.getCreatedAt());

        return discountDto;
    }
}
