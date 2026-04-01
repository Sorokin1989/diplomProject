package com.example.diplomproject.service;

import com.example.diplomproject.entity.Promocode;
import com.example.diplomproject.enums.DiscountType;
import com.example.diplomproject.repository.PromocodeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
@Service
public class PromocodeService {

    private final PromocodeRepository promocodeRepository;

    @Autowired
    public PromocodeService(PromocodeRepository promocodeRepository) {
        this.promocodeRepository = promocodeRepository;
    }

    // ==================== ОСНОВНЫЕ МЕТОДЫ ====================

    /**
     * Создание нового промокода (с параметрами).
     */
    public Promocode createPromoCode(String code,
                                     DiscountType discountType,
                                     BigDecimal value,
                                     BigDecimal minOrderAmount,
                                     Integer usageLimit,
                                     LocalDateTime validFrom,
                                     LocalDateTime validTo) {
        // Валидация обязательных полей
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("Код промокода не может быть пустым");
        }
        if (discountType == null) {
            throw new IllegalArgumentException("Тип скидки обязателен");
        }
        if (minOrderAmount == null || minOrderAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Минимальная сумма заказа должна быть указана и неотрицательна");
        }
        if (value == null) {
            throw new IllegalArgumentException("Значение скидки не может быть null");
        }

        // Валидация значения скидки в зависимости от типа
        if (discountType == DiscountType.PERCENT) {
            if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new IllegalArgumentException("Процент скидки должен быть от 0 до 100");
            }
        } else if (discountType == DiscountType.FIXED) {
            if (value.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Фиксированная скидка не может быть отрицательной");
            }
        } else {
            throw new IllegalArgumentException("Некорректный тип скидки");
        }

        // Проверка уникальности кода
        if (promocodeRepository.findByCode(code).isPresent()) {
            throw new IllegalArgumentException("Промокод с таким кодом уже существует");
        }

        // Установка значений по умолчанию
        if (usageLimit == null || usageLimit <= 0) {
            usageLimit = 100;
        }
        LocalDateTime now = LocalDateTime.now();
        if (validFrom == null) {
            validFrom = now;
        }
        if (validTo == null) {
            validTo = now.plusDays(30);
        }
        if (validTo.isBefore(validFrom)) {
            throw new IllegalArgumentException("Дата окончания не может быть раньше даты начала");
        }

        Promocode promocode = new Promocode();
        promocode.setCode(code.trim().toUpperCase());
        promocode.setDiscountType(discountType);
        promocode.setValue(value);
        promocode.setMinOrderAmount(minOrderAmount);
        promocode.setValidFrom(validFrom);
        promocode.setValidTo(validTo);
        promocode.setUsageLimit(usageLimit);
        promocode.setUsedCount(0);
        promocode.setActive(true);

        log.info("Создан промокод: {}", promocode.getCode());
        return promocodeRepository.save(promocode);
    }

    /**
     * Создание промокода из объекта (удобно для @ModelAttribute).
     */
    public Promocode createPromoCode(Promocode promocode) {
        return createPromoCode(
                promocode.getCode(),
                promocode.getDiscountType(),
                promocode.getValue(),
                promocode.getMinOrderAmount(),
                promocode.getUsageLimit(),
                promocode.getValidFrom(),
                promocode.getValidTo()
        );
    }

    /**
     * Применение промокода к цене с автоматическим увеличением счётчика.
     */
    @Transactional
    public BigDecimal applyPromocode(BigDecimal price, String code) {
        Promocode promocode = promocodeRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Промокод не найден"));

        if (promocode.getMinOrderAmount() == null || promocode.getDiscountType() == null) {
            throw new IllegalStateException("Промокод настроен некорректно: отсутствуют обязательные поля");
        }

        if (!isPromocodeActive(promocode)) {
            throw new IllegalArgumentException("Промокод недействителен или истёк");
        }

        if (price.compareTo(promocode.getMinOrderAmount()) < 0) {
            throw new IllegalArgumentException("Сумма заказа ниже минимальной для применения промокода");
        }

        BigDecimal discount = calculateDiscount(price, promocode);
        BigDecimal discountedPrice = price.subtract(discount);
        if (discountedPrice.compareTo(BigDecimal.ZERO) < 0) {
            discountedPrice = BigDecimal.ZERO;
        }

        promocode.setUsedCount(promocode.getUsedCount() + 1);
        promocodeRepository.save(promocode);
        log.info("Применён промокод {} к цене {}. Новая цена: {}", code, price, discountedPrice);

        return discountedPrice;
    }

    // ==================== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ====================

    private BigDecimal calculateDiscount(BigDecimal price, Promocode promocode) {
        BigDecimal discountValue = promocode.getValue();
        if (promocode.getDiscountType() == DiscountType.PERCENT) {
            BigDecimal percent = discountValue.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            return price.multiply(percent).setScale(2, RoundingMode.HALF_UP);
        } else {
            return discountValue;
        }
    }

    public boolean isPromocodeActive(Promocode promocode) {
        if (promocode == null) return false;
        LocalDateTime now = LocalDateTime.now();
        return promocode.isActive()
                && now.isAfter(promocode.getValidFrom())
                && now.isBefore(promocode.getValidTo())
                && promocode.getUsedCount() < promocode.getUsageLimit();
    }

    @Transactional
    public void incrementUsage(String code) {
        Promocode promocode = promocodeRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Промокод не найден"));
        promocode.setUsedCount(promocode.getUsedCount() + 1);
        promocodeRepository.save(promocode);
    }

    public List<Promocode> getAllPromocodes() {
        return promocodeRepository.findAll();
    }

    public Promocode getPromocodeById(Long id) {
        return promocodeRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Промокод отсутствует"));
    }

    /**
     * Обновление промокода (с параметрами).
     */
    @Transactional
    public void updatePromocode(Long id, String code, DiscountType discountType,
                                BigDecimal value, BigDecimal minOrderAmount,
                                Integer usageLimit, LocalDateTime validFrom,
                                LocalDateTime validTo, boolean active) {
        Promocode existing = getPromocodeById(id);
        existing.setCode(code.trim().toUpperCase());
        existing.setDiscountType(discountType);
        existing.setValue(value);
        existing.setMinOrderAmount(minOrderAmount);
        existing.setValidFrom(validFrom);
        existing.setValidTo(validTo);
        existing.setUsageLimit(usageLimit);
        existing.setActive(active);
        promocodeRepository.save(existing);
        log.info("Обновлён промокод id={}", id);
    }

    /**
     * Обновление промокода из объекта (удобно для @ModelAttribute).
     */
    @Transactional
    public void updatePromocode(Long id, Promocode promocode) {
        updatePromocode(
                id,
                promocode.getCode(),
                promocode.getDiscountType(),
                promocode.getValue(),
                promocode.getMinOrderAmount(),
                promocode.getUsageLimit(),
                promocode.getValidFrom(),
                promocode.getValidTo(),
                promocode.isActive()
        );
    }

    @Transactional
    public void deletePromocode(Long id) {
        promocodeRepository.deleteById(id);
        log.info("Удалён промокод id={}", id);
    }
}