package com.example.diplomproject.mapper;

import com.example.diplomproject.dto.BonusDto;
import com.example.diplomproject.entity.Bonus;
import org.springframework.stereotype.Component;

@Component
public class BonusMapper {

    /**
     * Преобразует сущность Bonus в DTO для безопасной передачи через API.
     * @param bonus JPA-сущность бонуса (может быть null)
     * @return BonusDto или null, если входной объект null
     */
    public BonusDto toBonusDTO(Bonus bonus) {
        if (bonus == null) {
            return null;
        }
        BonusDto bonusDto = new BonusDto();
        bonusDto.setId(bonus.getId());
        bonusDto.setTitle(bonus.getTitle());
        bonusDto.setDescription(bonus.getDescription());
        bonusDto.setUrl(bonus.getUrl());

        if (bonus.getCourse() != null) {
            bonusDto.setCourseId(bonus.getCourse().getId());
            bonusDto.setCourseTitle(bonus.getCourse().getTitle());
        }
        if (bonus.getUser() != null) {
            bonusDto.setUserId(bonus.getUser().getId());
        }
        return bonusDto;
    }

    /**
     * Преобразует DTO в сущность Bonus.
     * Связи (User, Course) устанавливаются в сервисе — не здесь.
     * @param bonusDto объект DTO (может быть null)
     * @return Bonus или null
     */

    public Bonus fromBonusDTOToEntity(BonusDto bonusDto){

        if( bonusDto==null) {
            return null;
        }
            Bonus bonus=new Bonus();
            bonus.setId(bonusDto.getId());
            bonus.setTitle(bonusDto.getTitle());
            bonus.setDescription(bonusDto.getDescription());
            bonus.setUrl(bonusDto.getUrl());

            return bonus;


    }
}
