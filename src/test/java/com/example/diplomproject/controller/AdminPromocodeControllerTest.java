package com.example.diplomproject.controller;

import com.example.diplomproject.entity.Promocode;
import com.example.diplomproject.service.PromocodeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AdminPromocodeControllerTest {

    @Mock
    private PromocodeService promocodeService;

    @InjectMocks
    private AdminPromocodeController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    // ---------- GET /admin/promocodes ----------
    @Test
    void listPromocodes_shouldReturnViewWithPromocodes() throws Exception {
        List<Promocode> promocodes = Arrays.asList(new Promocode(), new Promocode());
        when(promocodeService.getAllPromocodes()).thenReturn(promocodes);

        mockMvc.perform(get("/admin/promocodes"))
                .andExpect(status().isOk())
                .andExpect(view().name("layouts/main"))
                .andExpect(model().attributeExists("promocodes"))
                .andExpect(model().attribute("title", "Управление промокодами"))
                .andExpect(model().attribute("content", "pages/admin/promocodes/admin-list :: admin-promocodes-content"));

        verify(promocodeService).getAllPromocodes();
    }

    // ---------- GET /admin/promocodes/new ----------
    @Test
    void showCreateForm_shouldReturnForm() throws Exception {
        mockMvc.perform(get("/admin/promocodes/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("layouts/main"))
                .andExpect(model().attributeExists("promocode", "discountTypes"))
                .andExpect(model().attribute("title", "Создание промокода"))
                .andExpect(model().attribute("content", "pages/admin/promocodes/form :: promo-form"));
    }

    // ---------- POST /admin/promocodes ----------
    @Test
    void createPromocode_success() throws Exception {
        Promocode savedPromocode = new Promocode();
        savedPromocode.setId(1L);
        when(promocodeService.createPromoCode(any(Promocode.class))).thenReturn(savedPromocode);

        mockMvc.perform(post("/admin/promocodes")
                        .param("code", "TEST10")
                        .param("discountType", "PERCENT")
                        .param("discountValue", "10"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/admin/promocodes*"));
        // без проверки flash, так как их нет
        verify(promocodeService).createPromoCode(any(Promocode.class));
    }
    @Test
    void createPromocode_failure() throws Exception {
        when(promocodeService.createPromoCode(any(Promocode.class)))
                .thenThrow(new RuntimeException("DB error"));

        mockMvc.perform(post("/admin/promocodes")
                        .param("code", "TEST10")
                        .param("discountType", "PERCENT")
                        .param("discountValue", "10"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/admin/promocodes/new*"));
        // без проверки flash
    }

    // ---------- GET /admin/promocodes/edit/{id} ----------
    @Test
    void showEditForm_success() throws Exception {
        Promocode promocode = new Promocode();
        promocode.setId(1L);
        when(promocodeService.getPromocodeById(1L)).thenReturn(promocode);

        mockMvc.perform(get("/admin/promocodes/edit/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("layouts/main"))
                .andExpect(model().attributeExists("promocode", "discountTypes"))
                .andExpect(model().attribute("title", "Редактирование промокода"))
                .andExpect(model().attribute("content", "pages/admin/promocodes/form :: promo-form"));
    }

    @Test
    void showEditForm_notFound() throws Exception {
        when(promocodeService.getPromocodeById(99L)).thenThrow(new NoSuchElementException());

        mockMvc.perform(get("/admin/promocodes/edit/99"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/promocodes"))
                .andExpect(flash().attribute("error", "Промокод не найден"));
    }

    // ---------- POST /admin/promocodes/{id} ----------
    @Test
    void updatePromocode_success() throws Exception {
        doNothing().when(promocodeService).updatePromocode(eq(1L), any(Promocode.class));

        mockMvc.perform(post("/admin/promocodes/1")
                        .param("code", "UPDATED")
                        .param("discountType", "FIXED")
                        .param("discountValue", "500"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/admin/promocodes*"))
                .andExpect(flash().attribute("success", "Промокод обновлён"));
    }

    @Test
    void updatePromocode_notFound() throws Exception {
        doThrow(new NoSuchElementException()).when(promocodeService).updatePromocode(eq(99L), any(Promocode.class));

        mockMvc.perform(post("/admin/promocodes/99")
                        .param("code", "UPDATED")
                        .param("discountType", "FIXED")
                        .param("discountValue", "500"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/promocodes"))
                .andExpect(flash().attribute("error", "Промокод не найден"));
    }

    @Test
    void updatePromocode_failure() throws Exception {
        doThrow(new RuntimeException("Update error")).when(promocodeService).updatePromocode(eq(1L), any(Promocode.class));

        mockMvc.perform(post("/admin/promocodes/1")
                        .param("code", "UPDATED")
                        .param("discountType", "FIXED")
                        .param("discountValue", "500"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/admin/promocodes/edit/1*"))
                .andExpect(flash().attributeExists("error"));
    }

    // ---------- POST /admin/promocodes/delete/{id} ----------
    @Test
    void deletePromocode_success() throws Exception {
        doNothing().when(promocodeService).deletePromocode(1L);

        mockMvc.perform(post("/admin/promocodes/delete/1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/admin/promocodes*"))
                .andExpect(flash().attribute("success", "Промокод удалён"));
    }

    @Test
    void deletePromocode_notFound() throws Exception {
        doThrow(new NoSuchElementException()).when(promocodeService).deletePromocode(99L);

        mockMvc.perform(post("/admin/promocodes/delete/99"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/promocodes"))
                .andExpect(flash().attribute("error", "Промокод не найден"));
    }

    @Test
    void deletePromocode_failure() throws Exception {
        doThrow(new RuntimeException("Delete error")).when(promocodeService).deletePromocode(1L);

        mockMvc.perform(post("/admin/promocodes/delete/1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/admin/promocodes*"))
                .andExpect(flash().attributeExists("error"));
    }
}