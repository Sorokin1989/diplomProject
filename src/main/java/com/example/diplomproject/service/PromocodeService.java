package com.example.diplomproject.service;

import com.example.diplomproject.entity.Promocode;
import com.example.diplomproject.enums.DiscountType;
import com.example.diplomproject.repository.PromocodeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
public class PromocodeService {

    @Autowired
    private PromocodeRepository promocodeRepository;

    /**
     * Создание нового промокода.
     * @param code            код промокода (будет приведён к верхнему регистру и обрезан)
     * @param discountType    тип скидки (PERCENT или FIXED)
     * @param value           значение скидки (для PERCENT от 0 до 100, для FIXED >= 0)
     * @param minOrderAmount  минимальная сумма заказа для применения
     * @param usageLimit      максимальное количество использований (если null, будет установлено значение по умолчанию)
     * @param validFrom       дата начала действия (если null, будет установлено текущее время)
     * @param validTo         дата окончания действия (если null, будет установлено текущее время + 30 дней)
     * @return сохранённый промокод
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

        // Установка значений по умолчанию для необязательных параметров
        if (usageLimit == null || usageLimit <= 0) {
            usageLimit = 100; // по умолчанию 100 использований
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

        return promocodeRepository.save(promocode);
    }

    /**
     * Применение промокода к цене с автоматическим увеличением счётчика использований.
     * Метод выполняется в транзакции, что гарантирует атомарность проверок и обновления.
     * @param price исходная цена заказа
     * @param code  код промокода
     * @return цена после применения скидки (не может быть меньше 0)
     */
    @Transactional
    public BigDecimal applyPromocode(BigDecimal price, String code) {
        // Получаем промокод
        Promocode promocode = promocodeRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Промокод не найден"));

        // Проверка на null обязательных полей (защита от некорректных данных в БД)
        if (promocode.getMinOrderAmount() == null || promocode.getDiscountType() == null) {
            throw new IllegalStateException("Промокод настроен некорректно: отсутствуют обязательные поля");
        }

        // Проверка активности
        if (!isPromocodeActive(promocode)) {
            throw new IllegalArgumentException("Промокод недействителен или истёк");
        }

        // Проверка минимальной суммы заказа
        if (price.compareTo(promocode.getMinOrderAmount()) < 0) {
            throw new IllegalArgumentException("Сумма заказа ниже минимальной для применения промокода");
        }

        // Расчёт скидки
        BigDecimal discount = calculateDiscount(price, promocode);
        BigDecimal discountedPrice = price.subtract(discount);
        if (discountedPrice.compareTo(BigDecimal.ZERO) < 0) {
            discountedPrice = BigDecimal.ZERO;
        }

        // Увеличиваем счётчик использований
        promocode.setUsedCount(promocode.getUsedCount() + 1);
        promocodeRepository.save(promocode);

        return discountedPrice;
    }

    /**
     * Расчёт суммы скидки в зависимости от типа промокода.
     * @param price     исходная цена
     * @param promocode промокод
     * @return сумма скидки
     */
    private BigDecimal calculateDiscount(BigDecimal price, Promocode promocode) {
        BigDecimal discountValue = promocode.getValue();
        if (promocode.getDiscountType() == DiscountType.PERCENT) {
            // Сначала делим процент на 100 для повышения точности, затем умножаем на цену
            BigDecimal percent = discountValue.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            return price.multiply(percent).setScale(2, RoundingMode.HALF_UP);
        } else { // FIXED
            return discountValue;
        }
    }

    /**
     * Проверка, активен ли промокод в текущий момент.
     * Учитываются поля active, validFrom, validTo, а также лимит использований.
     * @param promocode промокод
     * @return true, если промокод активен и может быть применён
     */
    public boolean isPromocodeActive(Promocode promocode) {
        if (promocode == null) return false;
        LocalDateTime now = LocalDateTime.now();
        return promocode.isActive()
                && now.isAfter(promocode.getValidFrom())
                && now.isBefore(promocode.getValidTo())
                && promocode.getUsedCount() < promocode.getUsageLimit();
    }

    // ------------------------------------------------------------
    // Вспомогательные методы (если необходимы отдельно)
    // ------------------------------------------------------------

    /**
     * Увеличить счётчик использований промокода (без проверок активности).
     * <b>Внимание:</b> этот метод не содержит проверок активности и лимита,
     * он предназначен только для случаев, когда применение уже проверено отдельно.
     * Для обычного сценария используйте {@link #applyPromocode(BigDecimal, String)}.
     */
    @Transactional
    public void incrementUsage(String code) {
        Promocode promocode = promocodeRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Промокод не найден"));
        promocode.setUsedCount(promocode.getUsedCount() + 1);
        promocodeRepository.save(promocode);
    }
}