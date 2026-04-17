package com.example.diplomproject.controller;

import com.example.diplomproject.entity.Discount;
import com.example.diplomproject.service.DiscountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AdminDiscountControllerTest {

    @Mock
    private DiscountService discountService;

    @InjectMocks
    private AdminDiscountController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    // ---------- GET /admin/discounts ----------
    @Test
    void listDiscounts_shouldReturnViewWithDiscounts() throws Exception {
        List<Discount> discounts = List.of(new Discount(), new Discount());
        when(discountService.getAllDiscounts()).thenReturn(discounts);

        mockMvc.perform(get("/admin/discounts"))
                .andExpect(status().isOk())
                .andExpect(view().name("layouts/main"))
                .andExpect(model().attributeExists("discounts"))
                .andExpect(model().attribute("title", "Управление скидками"))
                .andExpect(model().attribute("content", "pages/admin/discounts/admin-list :: admin-discounts-content"));

        verify(discountService).getAllDiscounts();
    }

    // ---------- GET /admin/discounts/new ----------
    @Test
    void showCreateForm_shouldReturnForm() throws Exception {
        mockMvc.perform(get("/admin/discounts/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("layouts/main"))
                .andExpect(model().attributeExists("discount", "discountTypes"))
                .andExpect(model().attribute("title", "Создание скидки"))
                .andExpect(model().attribute("content", "pages/admin/discounts/form :: discount-form"));
    }

    // ---------- POST /admin/discounts ----------
    @Test
    void createDiscount_success() throws Exception {
        // Предполагаем, что createNewDiscount возвращает Discount (не void)
        Discount saved = new Discount();
        saved.setId(1L);
        when(discountService.createNewDiscount(any(Discount.class))).thenReturn(saved);

        mockMvc.perform(post("/admin/discounts")
                        .param("name", "Test Discount")
                        .param("discountPercent", "10"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/admin/discounts*"));

        verify(discountService).createNewDiscount(any(Discount.class));
    }

    @Test
    void createDiscount_failure() throws Exception {
        when(discountService.createNewDiscount(any(Discount.class)))
                .thenThrow(new RuntimeException("DB error"));

        mockMvc.perform(post("/admin/discounts")
                        .param("name", "Test Discount")
                        .param("discountPercent", "10"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/admin/discounts/new*"));
    }

    // ---------- GET /admin/discounts/edit/{id} ----------
    @Test
    void showEditForm_success() throws Exception {
        Discount discount = new Discount();
        discount.setId(1L);
        when(discountService.getDiscountById(1L)).thenReturn(discount);

        mockMvc.perform(get("/admin/discounts/edit/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("layouts/main"))
                .andExpect(model().attributeExists("discount", "discountTypes"))
                .andExpect(model().attribute("title", "Редактирование скидки"))
                .andExpect(model().attribute("content", "pages/admin/discounts/form :: discount-form"));
    }

    // ---------- POST /admin/discounts/{id} ----------
    @Test
    void updateDiscount_success() throws Exception {
        // Предполагаем, что updateDiscount возвращает Discount
        Discount updated = new Discount();
        when(discountService.updateDiscount(eq(1L), any(Discount.class))).thenReturn(updated);

        mockMvc.perform(post("/admin/discounts/1")
                        .param("name", "Updated Discount")
                        .param("discountPercent", "15"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/admin/discounts*"));

        verify(discountService).updateDiscount(eq(1L), any(Discount.class));
    }

    @Test
    void updateDiscount_failure() throws Exception {
        when(discountService.updateDiscount(eq(1L), any(Discount.class)))
                .thenThrow(new RuntimeException("Update error"));

        mockMvc.perform(post("/admin/discounts/1")
                        .param("name", "Updated Discount")
                        .param("discountPercent", "15"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/admin/discounts/edit/1*"));
    }

    // ---------- POST /admin/discounts/delete/{id} ----------
    @Test
    void deleteDiscount_success() throws Exception {
        // Предполагаем, что deleteDiscount возвращает void или что-то, но мы используем doNothing
        doNothing().when(discountService).deleteDiscount(1L);

        mockMvc.perform(post("/admin/discounts/delete/1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/admin/discounts*"));

        verify(discountService).deleteDiscount(1L);
    }

    @Test
    void deleteDiscount_failure() throws Exception {
        doThrow(new RuntimeException("Delete error")).when(discountService).deleteDiscount(1L);

        mockMvc.perform(post("/admin/discounts/delete/1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/admin/discounts*"));
    }
}