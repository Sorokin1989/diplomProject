package com.example.diplomproject.service;

import com.example.diplomproject.dto.DiscountDto;
import com.example.diplomproject.entity.Discount;
import com.example.diplomproject.enums.DiscountType;
import com.example.diplomproject.mapper.DiscountMapper;
import com.example.diplomproject.repository.DiscountRepository;
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
class DiscountServiceTest {

    @Mock
    private DiscountRepository discountRepository;

    @Mock
    private DiscountMapper discountMapper;

    @InjectMocks
    private DiscountService discountService;

    private Discount discount;
    private DiscountDto discountDto;

    @BeforeEach
    void setUp() {
        discount = new Discount();
        discount.setId(1L);
        discount.setTitle("Summer Sale");
        discount.setDescription("20% off");
        discount.setDiscountType(DiscountType.PERCENT);
        discount.setDiscountValue(BigDecimal.valueOf(20));
        discount.setMinOrderAmount(BigDecimal.valueOf(500));
        discount.setStartDate(LocalDateTime.now().minusDays(1));
        discount.setEndDate(LocalDateTime.now().plusDays(30));
        discount.setActive(true);

        discountDto = new DiscountDto();
        discountDto.setId(1L);
        discountDto.setTitle("Summer Sale");
        discountDto.setDiscountType("PERCENT");
        discountDto.setDiscountValue(BigDecimal.valueOf(20));
    }

    // ========== getAllDiscounts ==========
    @Test
    void getAllDiscounts_shouldReturnList() {
        when(discountRepository.findAll()).thenReturn(List.of(discount));
        List<Discount> result = discountService.getAllDiscounts();
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(discount);
    }

    // ========== getDiscountById ==========
    @Test
    void getDiscountById_shouldReturnDiscount() {
        when(discountRepository.findById(1L)).thenReturn(Optional.of(discount));
        Discount result = discountService.getDiscountById(1L);
        assertThat(result).isEqualTo(discount);
    }

    @Test
    void getDiscountById_shouldThrowWhenNotFound() {
        when(discountRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> discountService.getDiscountById(99L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("Скидка не найдена");
    }

    // ========== createNewDiscount ==========
    @Test
    void createNewDiscount_shouldSaveValidDiscount() {
        when(discountRepository.save(any(Discount.class))).thenReturn(discount);
        Discount saved = discountService.createNewDiscount(discount);
        assertThat(saved).isEqualTo(discount);
        verify(discountRepository).save(discount);
    }

    @Test
    void createNewDiscount_shouldThrowWhenTitleEmpty() {
        discount.setTitle("");
        assertThatThrownBy(() -> discountService.createNewDiscount(discount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Название скидки обязательно");
        verify(discountRepository, never()).save(any());
    }

    @Test
    void createNewDiscount_shouldThrowWhenDiscountTypeNull() {
        discount.setDiscountType(null);
        assertThatThrownBy(() -> discountService.createNewDiscount(discount))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createNewDiscount_shouldThrowWhenPercentExceeds100() {
        discount.setDiscountType(DiscountType.PERCENT);
        discount.setDiscountValue(BigDecimal.valueOf(150));
        assertThatThrownBy(() -> discountService.createNewDiscount(discount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Процент скидки не может превышать 100");
    }

    // ========== applyDiscount ==========
    @Test
    void applyDiscount_percent_shouldReducePrice() {
        BigDecimal result = discountService.applyDiscount(BigDecimal.valueOf(100), discount);
        assertThat(result).isEqualByComparingTo("80.00");
    }

    @Test
    void applyDiscount_fixed_shouldSubtract() {
        Discount fixed = new Discount();
        fixed.setDiscountType(DiscountType.FIXED);
        fixed.setDiscountValue(BigDecimal.valueOf(30));
        fixed.setActive(true);
        fixed.setStartDate(LocalDateTime.now().minusDays(1));
        fixed.setEndDate(LocalDateTime.now().plusDays(1));
        BigDecimal result = discountService.applyDiscount(BigDecimal.valueOf(100), fixed);
        assertThat(result).isEqualByComparingTo("70.00");
    }

    @Test
    void applyDiscount_fixed_shouldNotGoBelowZero() {
        Discount fixed = new Discount();
        fixed.setDiscountType(DiscountType.FIXED);
        fixed.setDiscountValue(BigDecimal.valueOf(150));
        fixed.setActive(true);
        fixed.setStartDate(LocalDateTime.now().minusDays(1));
        fixed.setEndDate(LocalDateTime.now().plusDays(1));
        BigDecimal result = discountService.applyDiscount(BigDecimal.valueOf(100), fixed);
        assertThat(result).isEqualByComparingTo("0.00");
    }

    @Test
    void applyDiscount_shouldReturnOriginalWhenDiscountInactive() {
        discount.setActive(false);
        BigDecimal result = discountService.applyDiscount(BigDecimal.valueOf(100), discount);
        assertThat(result).isEqualByComparingTo("100.00");
    }

    // ========== isDiscountActive ==========
    @Test
    void isDiscountActive_shouldReturnTrueWhenActiveAndDatesValid() {
        assertThat(discountService.isDiscountActive(discount)).isTrue();
    }

    @Test
    void isDiscountActive_shouldReturnFalseWhenNotActive() {
        discount.setActive(false);
        assertThat(discountService.isDiscountActive(discount)).isFalse();
    }

    @Test
    void isDiscountActive_shouldReturnFalseWhenStartDateInFuture() {
        discount.setStartDate(LocalDateTime.now().plusDays(1));
        assertThat(discountService.isDiscountActive(discount)).isFalse();
    }

    @Test
    void isDiscountActive_shouldReturnFalseWhenEndDateInPast() {
        discount.setEndDate(LocalDateTime.now().minusDays(1));
        assertThat(discountService.isDiscountActive(discount)).isFalse();
    }

    // ========== updateDiscount ==========
    @Test
    void updateDiscount_shouldUpdateFields() {
        Discount updatedData = new Discount();
        updatedData.setTitle("New Title");
        updatedData.setDescription("New Desc");
        updatedData.setDiscountType(DiscountType.FIXED);
        updatedData.setDiscountValue(BigDecimal.valueOf(50));
        updatedData.setMinOrderAmount(BigDecimal.valueOf(100));
        updatedData.setStartDate(LocalDateTime.now().minusDays(5));
        updatedData.setEndDate(LocalDateTime.now().plusDays(20));
        updatedData.setActive(false);

        when(discountRepository.findById(1L)).thenReturn(Optional.of(discount));
        when(discountRepository.save(any(Discount.class))).thenReturn(discount);

        Discount result = discountService.updateDiscount(1L, updatedData);

        assertThat(result.getTitle()).isEqualTo("New Title");
        assertThat(result.getDiscountType()).isEqualTo(DiscountType.FIXED);
        assertThat(result.getDiscountValue()).isEqualByComparingTo("50");
        verify(discountRepository).save(discount);
    }

    @Test
    void updateDiscount_shouldThrowWhenPercentExceeds100() {
        Discount updatedData = new Discount();
        updatedData.setDiscountType(DiscountType.PERCENT);
        updatedData.setDiscountValue(BigDecimal.valueOf(200));
        when(discountRepository.findById(1L)).thenReturn(Optional.of(discount));
        assertThatThrownBy(() -> discountService.updateDiscount(1L, updatedData))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Процент скидки не может превышать 100");
    }

    // ========== deleteDiscount ==========
    @Test
    void deleteDiscount_shouldDelete() {
        when(discountRepository.findById(1L)).thenReturn(Optional.of(discount));
        discountService.deleteDiscount(1L);
        verify(discountRepository).delete(discount);
    }

    @Test
    void deleteDiscount_shouldThrowWhenNotFound() {
        when(discountRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> discountService.deleteDiscount(99L))
                .isInstanceOf(NoSuchElementException.class);
        verify(discountRepository, never()).delete(any());
    }

    // ========== getAllActiveDiscountsDto ==========
    @Test
    void getAllActiveDiscountsDto_shouldReturnMappedList() {
        LocalDateTime now = LocalDateTime.now();
        when(discountRepository.findActiveAtDate(any(LocalDateTime.class))).thenReturn(List.of(discount));
        when(discountMapper.toDiscountDto(discount)).thenReturn(discountDto);

        List<DiscountDto> result = discountService.getAllActiveDiscountsDto();

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(discountDto);
        verify(discountMapper).toDiscountDto(discount);
    }
}