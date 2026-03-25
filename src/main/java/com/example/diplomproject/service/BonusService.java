package com.example.diplomproject.service;

import com.example.diplomproject.entity.User;
import com.example.diplomproject.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BonusService {
    private final UserRepository userRepository;

    @Autowired
    public BonusService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    private static final int BONUS_FOR_REVIEW = 10;
    private static final int BONUS_FOR_FIRST_ORDER=100;
    private static final int BONUS_FOR_REGISTRATION=30;


    /**
     * Начисление бонусов за регистрацию
     */

    @Transactional
    public void addBonusForRegistration(User user){
        if (user != null && user.getId() != null){
            user.setBonusPoints(user.getBonusPoints()+BONUS_FOR_REGISTRATION);
            userRepository.save(user);
        }
    }


    /**
     * Начисление бонусов за оставленный отзыв
     */

    @Transactional
    public void addBonusForReview(User user) {
        if(user!=null && user.getId()!= null){
            user.setBonusPoints(user.getBonusPoints() + BONUS_FOR_REVIEW);
            userRepository.save(user);
        }
    }

    /**
     * Начисление бонусов за первый заказ
     */
    @Transactional
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
    @Transactional
        public boolean spendBonusPoints(User user, int bonusPoints){

        if (user==null || user.getId()==null){
            return false;
        }

            if(user.getBonusPoints()>=bonusPoints){
                user.setBonusPoints(user.getBonusPoints()-bonusPoints);
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
