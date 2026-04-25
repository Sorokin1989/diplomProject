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
    @Transactional
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
    @Transactional
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

        // Проверяем, можно ли применить промокод
        if (!isPromocodeActive(promocode) || price.compareTo(promocode.getMinOrderAmount()) < 0) {
            throw new IllegalArgumentException("Промокод недействителен или не подходит для данной суммы");
        }

        BigDecimal discountAmount = calculateDiscount(price, promocode);
        BigDecimal finalPrice=price.subtract(discountAmount).max(BigDecimal.ZERO);
        // Увеличиваем счётчик ТОЛЬКО при реальном применении
        promocode.setUsedCount(promocode.getUsedCount() + 1);
        promocodeRepository.save(promocode);
        return finalPrice;
    }

    // ==================== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ====================

    public BigDecimal calculateDiscount(BigDecimal price, Promocode promocode) {
        if (!isPromocodeActive(promocode) || price.compareTo(promocode.getMinOrderAmount()) < 0) {
            return BigDecimal.ZERO;
        }
        if (promocode.getDiscountType() == DiscountType.PERCENT) {
            // value хранит целое число процентов (например, 50)
            return price.multiply(promocode.getValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } else {
            // фиксированная скидка
            BigDecimal discount = promocode.getValue();
            return discount.compareTo(price) > 0 ? price : discount;
        }
    }

    public BigDecimal calculatePriceAfterDiscount(BigDecimal price, Promocode promocode) {
        if (!isPromocodeActive(promocode) || price.compareTo(promocode.getMinOrderAmount()) < 0) {
            return price;
        }
        BigDecimal discount = calculateDiscount(price, promocode);
        BigDecimal result = price.subtract(discount);
        return result.max(BigDecimal.ZERO);
    }

    public boolean isPromocodeActive(Promocode promocode) {
        if (promocode == null) return false;
        LocalDateTime now = LocalDateTime.now();
        return promocode.isActive()
                && !now.isBefore(promocode.getValidFrom())   // now >= validFrom
                && now.isBefore(promocode.getValidTo())      // now < validTo
                && promocode.getUsedCount() < promocode.getUsageLimit();
    }
//    @Transactional
//    public void incrementUsage(String code) {
//        Promocode promocode = promocodeRepository.findByCode(code)
//                .orElseThrow(() -> new IllegalArgumentException("Промокод не найден"));
//        promocode.setUsedCount(promocode.getUsedCount() + 1);
//        promocodeRepository.save(promocode);
//    }

    @Transactional(readOnly = true)
    public List<Promocode> getAllPromocodes() {
        return promocodeRepository.findAll();
    }

    @Transactional(readOnly = true)
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

        if (code != null && !code.isBlank()) {
            existing.setCode(normalizeCode(code));
        }
        if (discountType != null) {
            existing.setDiscountType(discountType);
        }
        if (value != null) {
            existing.setValue(value);
        }
        if (minOrderAmount != null) {
            existing.setMinOrderAmount(minOrderAmount);
        }
        if (usageLimit != null) {
            existing.setUsageLimit(usageLimit);
        }
        if (validFrom != null) {
            existing.setValidFrom(validFrom);
        }
        if (validTo != null) {
            existing.setValidTo(validTo);
        }
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
        Promocode promocode = getPromocodeById(id);
        promocode.setActive(false);
        promocodeRepository.save(promocode);
        log.info("Промокод id={} деактивирован", id);
    }

    @Transactional(readOnly = true)
    public Promocode findByCode(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        return promocodeRepository.findByCode(code).orElse(null);
    }

    @Transactional(readOnly = true)
    public Promocode save(Promocode promocode) {
        return promocodeRepository.save(promocode);
    }

//    public boolean isValid(Promocode promo, BigDecimal orderTotal) {
//        LocalDateTime now = LocalDateTime.now();
//        return promo.isActive() &&
//                promo.getValidFrom().isBefore(now) &&
//                promo.getValidTo().isAfter(now) &&
//                promo.getUsedCount() < promo.getUsageLimit() &&
//                orderTotal.compareTo(promo.getMinOrderAmount()) >= 0;
//    }
//
//    public BigDecimal applyDiscount(BigDecimal originalTotal, Promocode promo) {
//        if (promo.getDiscountType() == DiscountType.PERCENT) {
//            return originalTotal.multiply(BigDecimal.ONE.subtract(
//                    promo.getValue().divide(BigDecimal.valueOf(100))));
//        } else { // FIXED
//            BigDecimal discounted = originalTotal.subtract(promo.getValue());
//            return discounted.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : discounted;
//        }
//    }
private String normalizeCode(String code) {
    return code.trim().toUpperCase();
}
}