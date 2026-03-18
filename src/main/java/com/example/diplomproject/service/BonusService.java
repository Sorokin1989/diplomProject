package com.example.diplomproject.service;

import com.example.diplomproject.entity.User;
import com.example.diplomproject.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BonusService {

    @Autowired
    private UserRepository userRepository;

    private static final int BONUS_FOR_REVIEW = 10;
    private static final int BONUS_FOR_FIRST_ORDER=100;
    private static final int BONUS_FOR_REGISTRATION=30;


    /**
     * Начисление бонусов за оставленный отзыв
     */

    public void addBonusForReview(User user) {
        if(user!=null && user.getId()!= null){
            user.setBonusPoints(user.getBonusPoints() + BONUS_FOR_REVIEW);
            userRepository.save(user);
        }
    }
}
