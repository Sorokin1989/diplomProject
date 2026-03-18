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
     * Начисление бонусов за регистрацию
     */

    public void addBonusForRegistration(User user){
        if (user != null && user.getId() != null){
            user.setBonusPoints(user.getBonusPoints()+BONUS_FOR_REGISTRATION);
            userRepository.save(user);
        }
    }


    /**
     * Начисление бонусов за оставленный отзыв
     */

    public void addBonusForReview(User user) {
        if(user!=null && user.getId()!= null){
            user.setBonusPoints(user.getBonusPoints() + BONUS_FOR_REVIEW);
            userRepository.save(user);
        }
    }

    /**
     * Начисление бонусов за первый заказ
     */
    public void addBonusForFirstOrder (User user) {
        if (user != null && user.getId() != null && user.getOrders().size() == 1) {
            user.setBonusPoints(user.getBonusPoints() + BONUS_FOR_FIRST_ORDER);
            userRepository.save(user);
        }
    }

    /**
     * Списание бонусных баллов
     * @return true, если успешно списано, иначе false
     */
        public boolean spendBonusPoints(User user, int bonusPoint){
            if(user.getBonusPoints()>=bonusPoint){
                user.setBonusPoints(user.getBonusPoints()-bonusPoint);
                userRepository.save(user);
                return true;
            }
            return false;
        }
    /**
     * Получение текущего количества бонусов
     */

    public int getCurrentBonusPoints(User user){
        if (user!=null){
            return user.getBonusPoints();
        }
        return 0;
    }



}
