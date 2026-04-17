package com.example.diplomproject.mapper;

import com.example.diplomproject.dto.PromocodeDto;
import com.example.diplomproject.entity.Promocode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PromocodeMapper {

    private final CourseMapper courseMapper;
    private final CategoryMapper categoryMapper;

    @Autowired
    public PromocodeMapper(CourseMapper courseMapper, CategoryMapper categoryMapper) {
        this.courseMapper = courseMapper;
        this.categoryMapper = categoryMapper;
    }

    public PromocodeDto toPromoCodeDTO(Promocode promocode) {

        if (promocode == null) {
            return null;
        }

        PromocodeDto promocodeDto = new PromocodeDto();
        promocodeDto.setId(promocode.getId());
        promocodeDto.setCode(promocode.getCode());
        promocodeDto.setDiscountType(String.valueOf(promocode.getDiscountType()));
        promocodeDto.setValue(promocode.getValue());
        promocodeDto.setMinOrderAmount(promocode.getMinOrderAmount());
        promocodeDto.setValidFrom(promocode.getValidFrom());
        promocodeDto.setValidTo(promocode.getValidTo());
        promocodeDto.setUsageLimit(promocode.getUsageLimit());
        promocodeDto.setUsedCount(promocode.getUsedCount());

        if (promocode.getApplicableCourses() != null) {
            promocodeDto.setApplicableCourseDtos(promocode.getApplicableCourses().stream().map(course -> courseMapper.toCourseDto(course)).toList());
        }

        if (promocode.getApplicableCategories() != null) {
            promocodeDto.setApplicableCategoryDtos(promocode.getApplicableCategories().stream().map(category -> categoryMapper.toCategoryDTO(category)).toList());
        }

        promocodeDto.setActive(promocode.isActive());
        promocodeDto.setCreatedAt(promocode.getCreatedAt());

        return promocodeDto;
    }
//    public Promocode fromPromoCodeDtoToEntity(PromocodeDto promocodeDto){
//        if(promocodeDto==null){
//            return  null;
//        }
//
//        Promocode promocode=new Promocode();
//        promocode.setId(promocodeDto.getId());
//        promocode.setCode(promocodeDto.getCode());
//        promocode.setDiscountType(DiscountType.valueOf(promocodeDto.getDiscountType()));
//        promocode.setValue(promocodeDto.getValue());
//        promocode.setMinOrderAmount(promocodeDto.getMinOrderAmount());
//        promocode.setValidFrom(promocodeDto.getValidFrom());
//        promocode.setValidTo(promocodeDto.getValidTo());
//        promocode.setUsageLimit(promocodeDto.getUsageLimit());
//        promocode.setUsedCount(promocodeDto.getUsedCount());
//        promocode.setActive(promocodeDto.isActive());
//        promocode.setCreatedAt(promocodeDto.getCreatedAt());
//        return promocode;
//    }
}
