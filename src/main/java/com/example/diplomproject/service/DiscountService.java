package com.example.diplomproject.service;

import com.example.diplomproject.entity.Discount;
import com.example.diplomproject.repository.DiscountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class DiscountService {

    private final DiscountRepository discountRepository;

    @Autowired
    public DiscountService(DiscountRepository discountRepository) {
        this.discountRepository = discountRepository;
    }

    /**
     * Получение всех скидок
     */

    public List<Discount> getAllDiscounts() {
        return discountRepository.findAll();
    }

    /**
     * Получение скидки по ID
     */

    public Discount getDiscountById(Long id) {
        return discountRepository.findById(id).
                orElseThrow(() ->
                        new NoSuchElementException("Скидка не найдена!"));
    }

    @Transactional
    public Discount createNewDiscount(Discount discount) {
        if (discount == null) {
            throw new IllegalArgumentException("Скидка не может быть null");
        }
        if (discount.getDiscountValue() == null || discount.getDiscountValue().
                compareTo(BigDecimal.ZERO) < 0 || discount.getDiscountValue().
                compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("Процент скидки должен лежать " +
                    "в диапазоне от 0 до 100");
        }
        if (discount.getEndDate() == null || discount.getEndDate().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Срок действия скидки должен быть в будущем");
        }
        return discountRepository.save(discount);

    }

    /**
     * Применение скидки к цене курса
     */
    public BigDecimal applyDiscount(BigDecimal originalPrice, Discount discount) {
        if (originalPrice == null) {
            throw new IllegalArgumentException("Цена не может быть null");
        }
        if (discount == null || !isDiscountActive(discount)) {
            return originalPrice; // Если скидка неактивна — возвращаем исходную цену
        }
        BigDecimal discountAmount = originalPrice.multiply(discount.getDiscountValue())
                .divide(new BigDecimal("100"), 2, BigDecimal.ROUND_HALF_UP);
        return originalPrice.subtract(discountAmount);
    }

    /**
     * Проверка, активна ли скидка
     */
    public boolean isDiscountActive(Discount discount) {
        if (discount == null) return false;
        return LocalDateTime.now().isBefore(discount.getEndDate());
    }

    /**
     * Обновление скидки
     */
    @Transactional
    public Discount updateDiscount(Long id, Discount updatedDiscount) {
        Discount existingDiscount = getDiscountById(id);

        if (updatedDiscount.getDiscountValue() != null &&
                updatedDiscount.getDiscountValue().compareTo(BigDecimal.ZERO) >= 0 &&
                updatedDiscount.getDiscountValue().compareTo(new BigDecimal("100")) <= 0) {
            existingDiscount.setDiscountValue(updatedDiscount.getDiscountValue());
        }

        if (updatedDiscount.getEndDate() != null &&
                updatedDiscount.getEndDate().isAfter(LocalDateTime.now())) {
            existingDiscount.setEndDate(updatedDiscount.getEndDate());
        } else {
            throw new IllegalArgumentException("Срок действия скидки должен быть в будущем");
        }

        return discountRepository.save(existingDiscount);
    }

    /**
     * Удаление скидки
     */
    @Transactional
    public void deleteDiscount(Long id) {
        Discount discount = getDiscountById(id);
        discountRepository.delete(discount);
    }

}





