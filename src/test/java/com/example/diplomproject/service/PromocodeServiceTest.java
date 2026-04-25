package com.example.diplomproject.service;

import com.example.diplomproject.entity.Promocode;
import com.example.diplomproject.enums.DiscountType;
import com.example.diplomproject.repository.PromocodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PromocodeServiceTest {

    @Mock
    private PromocodeRepository promocodeRepository;

    @InjectMocks
    private PromocodeService promocodeService;

    private Promocode activePromocode;
    private Promocode inactivePromocode;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();
        activePromocode = new Promocode();
        activePromocode.setId(1L);
        activePromocode.setCode("SAVE10");
        activePromocode.setDiscountType(DiscountType.PERCENT);
        activePromocode.setValue(BigDecimal.valueOf(10));
        activePromocode.setMinOrderAmount(BigDecimal.valueOf(50));
        activePromocode.setValidFrom(now.minusDays(1));
        activePromocode.setValidTo(now.plusDays(10));
        activePromocode.setUsageLimit(100);
        activePromocode.setUsedCount(5);
        activePromocode.setActive(true);

        inactivePromocode = new Promocode();
        inactivePromocode.setId(2L);
        inactivePromocode.setCode("EXPIRED");
        inactivePromocode.setDiscountType(DiscountType.FIXED);
        inactivePromocode.setValue(BigDecimal.valueOf(20));
        inactivePromocode.setMinOrderAmount(BigDecimal.ZERO);
        inactivePromocode.setValidFrom(now.minusDays(10));
        inactivePromocode.setValidTo(now.minusDays(1));
        inactivePromocode.setUsageLimit(10);
        inactivePromocode.setUsedCount(10);
        inactivePromocode.setActive(false);
    }

    // ========== createPromoCode ==========
    @Test
    void createPromoCode_shouldSaveValidPromocode() {
        when(promocodeRepository.findByCode("SAVE10")).thenReturn(Optional.empty());
        when(promocodeRepository.save(any(Promocode.class))).thenReturn(activePromocode);

        Promocode created = promocodeService.createPromoCode(
                "SAVE10", DiscountType.PERCENT, BigDecimal.valueOf(10),
                BigDecimal.valueOf(50), 100, now.minusDays(1), now.plusDays(10)
        );

        assertThat(created).isEqualTo(activePromocode);
        verify(promocodeRepository).save(any(Promocode.class));
    }

    @Test
    void createPromoCode_shouldThrowWhenCodeEmpty() {
        assertThatThrownBy(() -> promocodeService.createPromoCode("", DiscountType.PERCENT,
                BigDecimal.TEN, BigDecimal.ZERO, 100, now, now.plusDays(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Код промокода не может быть пустым");
        verify(promocodeRepository, never()).save(any());
    }

    @Test
    void createPromoCode_shouldThrowWhenDuplicateCode() {
        when(promocodeRepository.findByCode("SAVE10")).thenReturn(Optional.of(activePromocode));
        assertThatThrownBy(() -> promocodeService.createPromoCode("SAVE10", DiscountType.PERCENT,
                BigDecimal.TEN, BigDecimal.ZERO, 100, now, now.plusDays(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Промокод с таким кодом уже существует");
        verify(promocodeRepository, never()).save(any());
    }

    @Test
    void createPromoCode_shouldThrowWhenPercentOutOfRange() {
        assertThatThrownBy(() -> promocodeService.createPromoCode("BAD", DiscountType.PERCENT,
                BigDecimal.valueOf(150), BigDecimal.ZERO, 100, now, now.plusDays(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Процент скидки должен быть от 0 до 100");
    }

    // ========== applyPromocode ==========
    @Test
    void applyPromocode_shouldApplyPercentDiscount() {
        when(promocodeRepository.findByCode("SAVE10")).thenReturn(Optional.of(activePromocode));

        BigDecimal result = promocodeService.applyPromocode(BigDecimal.valueOf(100), "SAVE10");

        assertThat(result).isEqualByComparingTo("90.00");
        assertThat(activePromocode.getUsedCount()).isEqualTo(6); // increased
        verify(promocodeRepository).save(activePromocode);
    }

    @Test
    void applyPromocode_shouldThrowWhenPromocodeNotFound() {
        when(promocodeRepository.findByCode("NONEXIST")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> promocodeService.applyPromocode(BigDecimal.valueOf(100), "NONEXIST"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Промокод не найден");
        verify(promocodeRepository, never()).save(any());
    }

    @Test
    void applyPromocode_shouldThrowWhenOrderTotalBelowMin() {
        when(promocodeRepository.findByCode("SAVE10")).thenReturn(Optional.of(activePromocode));
        assertThatThrownBy(() -> promocodeService.applyPromocode(BigDecimal.valueOf(30), "SAVE10"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Промокод недействителен или не подходит для данной суммы");
        verify(promocodeRepository, never()).save(any());
    }

    @Test
    void applyPromocode_shouldThrowWhenPromocodeInactive() {
        when(promocodeRepository.findByCode("EXPIRED")).thenReturn(Optional.of(inactivePromocode));
        assertThatThrownBy(() -> promocodeService.applyPromocode(BigDecimal.valueOf(100), "EXPIRED"))
                .isInstanceOf(IllegalArgumentException.class);
        verify(promocodeRepository, never()).save(any());
    }

    // ========== calculateDiscount ==========
    @Test
    void calculateDiscount_shouldReturnZeroWhenInactive() {
        BigDecimal result = promocodeService.calculateDiscount(BigDecimal.valueOf(100), inactivePromocode);
        assertThat(result).isZero();
    }

    @Test
    void calculateDiscount_shouldReturnCorrectPercent() {
        BigDecimal result = promocodeService.calculateDiscount(BigDecimal.valueOf(100), activePromocode);
        assertThat(result).isEqualByComparingTo("10.00"); // ← исправлено с 90.00 на 10.00
    }

    @Test
    void calculateDiscount_shouldReturnZeroWhenBelowMin() {
        BigDecimal result = promocodeService.calculateDiscount(BigDecimal.valueOf(30), activePromocode);
        assertThat(result).isZero();
    }

    // ========== isPromocodeActive ==========
    @Test
    void isPromocodeActive_shouldReturnTrueForActive() {
        assertThat(promocodeService.isPromocodeActive(activePromocode)).isTrue();
    }

    @Test
    void isPromocodeActive_shouldReturnFalseWhenExpired() {
        assertThat(promocodeService.isPromocodeActive(inactivePromocode)).isFalse();
    }

    @Test
    void isPromocodeActive_shouldReturnFalseWhenUsageLimitReached() {
        activePromocode.setUsedCount(100);
        assertThat(promocodeService.isPromocodeActive(activePromocode)).isFalse();
    }

    // ========== getAllPromocodes ==========
    @Test
    void getAllPromocodes_shouldReturnList() {
        when(promocodeRepository.findAll()).thenReturn(List.of(activePromocode, inactivePromocode));
        List<Promocode> list = promocodeService.getAllPromocodes();
        assertThat(list).hasSize(2);
        verify(promocodeRepository).findAll();
    }

    // ========== getPromocodeById ==========
    @Test
    void getPromocodeById_shouldReturnPromocode() {
        when(promocodeRepository.findById(1L)).thenReturn(Optional.of(activePromocode));
        Promocode found = promocodeService.getPromocodeById(1L);
        assertThat(found).isEqualTo(activePromocode);
    }

    @Test
    void getPromocodeById_shouldThrowWhenNotFound() {
        when(promocodeRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> promocodeService.getPromocodeById(99L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("Промокод отсутствует");
    }

    // ========== updatePromocode ==========
    @Test
    void updatePromocode_shouldUpdateFields() {
        when(promocodeRepository.findById(1L)).thenReturn(Optional.of(activePromocode));
        when(promocodeRepository.save(any(Promocode.class))).thenReturn(activePromocode);

        promocodeService.updatePromocode(1L, "NEWCODE", DiscountType.FIXED,
                BigDecimal.valueOf(25), BigDecimal.valueOf(10),
                50, now.minusDays(2), now.plusDays(20), true);

        assertThat(activePromocode.getCode()).isEqualTo("NEWCODE");
        assertThat(activePromocode.getDiscountType()).isEqualTo(DiscountType.FIXED);
        assertThat(activePromocode.getValue()).isEqualByComparingTo("25");
        verify(promocodeRepository).save(activePromocode);
    }

    // ========== deletePromocode ==========
    @Test
    void deletePromocode_shouldDeactivate() {
        when(promocodeRepository.findById(1L)).thenReturn(Optional.of(activePromocode));
        when(promocodeRepository.save(any(Promocode.class))).thenReturn(activePromocode);

        promocodeService.deletePromocode(1L);

        assertThat(activePromocode.isActive()).isFalse();
        verify(promocodeRepository).save(activePromocode);
    }

    // ========== findByCode ==========
    @Test
    void findByCode_shouldReturnPromocodeWhenExists() {
        when(promocodeRepository.findByCode("SAVE10")).thenReturn(Optional.of(activePromocode));
        Promocode found = promocodeService.findByCode("SAVE10");
        assertThat(found).isEqualTo(activePromocode);
    }

    @Test
    void findByCode_shouldReturnNullWhenNotFound() {
        when(promocodeRepository.findByCode("NONE")).thenReturn(Optional.empty());
        Promocode found = promocodeService.findByCode("NONE");
        assertThat(found).isNull();
    }
}