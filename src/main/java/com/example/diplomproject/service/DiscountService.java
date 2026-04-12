package com.example.diplomproject.service;

import com.example.diplomproject.dto.DiscountDto;
import com.example.diplomproject.entity.Discount;
import com.example.diplomproject.enums.DiscountType;
import com.example.diplomproject.mapper.DiscountMapper;
import com.example.diplomproject.repository.DiscountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiscountService {

    @Autowired
    private final DiscountRepository discountRepository;

    @Autowired
    private final DiscountMapper discountMapper;


    public List<Discount> getAllDiscounts() {
        return discountRepository.findAll();
    }

    public Discount getDiscountById(Long id) {
        return discountRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Скидка не найдена"));
    }

    /**
     * Создание новой скидки.
     */
    @Transactional
    public Discount createNewDiscount(Discount discount) {
        if (discount == null) {
            throw new IllegalArgumentException("Скидка не может быть null");
        }
        // Валидация названия
        if (discount.getTitle() == null || discount.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Название скидки обязательно");
        }
        // Валидация типа и значения скидки
        if (discount.getDiscountType() == null) {
            throw new IllegalArgumentException("Тип скидки обязателен");
        }
        if (discount.getDiscountValue() == null || discount.getDiscountValue().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Значение скидки не может быть отрицательным");
        }
        if (discount.getDiscountType() == DiscountType.PERCENT) {
            if (discount.getDiscountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new IllegalArgumentException("Процент скидки не может превышать 100");
            }
        }
        // Установка значений по умолчанию
        if (discount.getMinOrderAmount() == null) {
            discount.setMinOrderAmount(BigDecimal.ZERO);
        }
        if (discount.getStartDate() == null) {
            discount.setStartDate(LocalDateTime.now());
        }
        if (discount.getEndDate() == null) {
            discount.setEndDate(LocalDateTime.now().plusDays(30));
        }
        if (discount.getEndDate().isBefore(discount.getStartDate())) {
            throw new IllegalArgumentException("Дата окончания не может быть раньше даты начала");
        }
        if (discount.getEndDate().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Дата окончания должна быть в будущем");
        }
        discount.setActive(true);
        log.info("Создана новая скидка: {}", discount.getTitle());
        return discountRepository.save(discount);
    }

    /**
     * Применение скидки к цене.
     */
    public BigDecimal applyDiscount(BigDecimal originalPrice, Discount discount) {
        if (originalPrice == null) {
            throw new IllegalArgumentException("Цена не может быть null");
        }
        if (discount == null || !isDiscountActive(discount)) {
            return originalPrice;
        }
        BigDecimal discountAmount;
        if (discount.getDiscountType() == DiscountType.PERCENT) {
            discountAmount = originalPrice.multiply(discount.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } else { // FIXED
            discountAmount = discount.getDiscountValue();
            if (discountAmount.compareTo(originalPrice) > 0) {
                discountAmount = originalPrice; // скидка не может сделать цену отрицательной
            }
        }
        return originalPrice.subtract(discountAmount).max(BigDecimal.ZERO);
    }

    /**
     * Проверка, активна ли скидка на текущий момент.
     */
    public boolean isDiscountActive(Discount discount) {
        if (discount == null) return false;
        LocalDateTime now = LocalDateTime.now();
        return discount.isActive()
                && (discount.getStartDate() == null || !now.isBefore(discount.getStartDate()))
                && (discount.getEndDate() == null || now.isBefore(discount.getEndDate()));
    }

    /**
     * Обновление скидки.
     */
    @Transactional
    public Discount updateDiscount(Long id, Discount updatedDiscount) {
        Discount existing = getDiscountById(id);

        if (updatedDiscount.getTitle() != null && !updatedDiscount.getTitle().trim().isEmpty()) {
            existing.setTitle(updatedDiscount.getTitle());
        }
        if (updatedDiscount.getDescription() != null) {
            existing.setDescription(updatedDiscount.getDescription());
        }
        if (updatedDiscount.getDiscountType() != null) {
            existing.setDiscountType(updatedDiscount.getDiscountType());
        }
        if (updatedDiscount.getDiscountValue() != null && updatedDiscount.getDiscountValue().compareTo(BigDecimal.ZERO) >= 0) {
            if (existing.getDiscountType() == DiscountType.PERCENT && updatedDiscount.getDiscountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new IllegalArgumentException("Процент скидки не может превышать 100");
            }
            existing.setDiscountValue(updatedDiscount.getDiscountValue());
        }
        if (updatedDiscount.getMinOrderAmount() != null && updatedDiscount.getMinOrderAmount().compareTo(BigDecimal.ZERO) >= 0) {
            existing.setMinOrderAmount(updatedDiscount.getMinOrderAmount());
        }
        if (updatedDiscount.getStartDate() != null) {
            existing.setStartDate(updatedDiscount.getStartDate());
        }
        if (updatedDiscount.getEndDate() != null) {
            if (updatedDiscount.getEndDate().isBefore(existing.getStartDate())) {
                throw new IllegalArgumentException("Дата окончания не может быть раньше даты начала");
            }
            existing.setEndDate(updatedDiscount.getEndDate());
        }
        existing.setActive(updatedDiscount.isActive());

        log.info("Обновлена скидка id={}", id);
        return discountRepository.save(existing);
    }

    /**
     * Удаление скидки.
     */
    @Transactional
    public void deleteDiscount(Long id) {
        Discount discount = getDiscountById(id);
        discountRepository.delete(discount);
        log.info("Удалена скидка id={}", id);
    }

    public List<DiscountDto> getAllActiveDiscountsDto() {
        LocalDateTime now = LocalDateTime.now();
        return discountRepository.findActiveAtDate(now)
                .stream()
                .map(discountMapper::toDiscountDto)
                .collect(Collectors.toList());
    }
}