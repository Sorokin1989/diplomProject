package com.example.diplomproject.controller;

import com.example.diplomproject.dto.CartDto;
import com.example.diplomproject.dto.CartItemDto;
import com.example.diplomproject.dto.CourseDto;
import com.example.diplomproject.entity.User;
import com.example.diplomproject.enums.Role;
import com.example.diplomproject.service.CartService;
import com.example.diplomproject.service.CourseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CartControllerTest {

    @Mock
    private CartService cartService;

    @Mock
    private CourseService courseService;

    @InjectMocks
    private CartController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    // Вспомогательный метод для установки аутентифицированного пользователя
    private void authenticateUser(User user) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities())
        );
    }

    private void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    // ---------- GET /cart ----------
    @Test
    void viewCart_authenticatedUser_shouldReturnCartPage() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setRole(Role.USER);   // обязательно устанавливаем роль
        authenticateUser(user);

        CartDto cartDto = new CartDto();
        cartDto.setCartItems(List.of(new CartItemDto(), new CartItemDto()));
        when(cartService.getOrCreateCartDto(any(User.class))).thenReturn(cartDto);
        when(cartService.getTotalPriceDto(any(User.class))).thenReturn(BigDecimal.valueOf(100));

        mockMvc.perform(get("/cart"))
                .andExpect(status().isOk())
                .andExpect(view().name("layouts/main"))
                .andExpect(model().attributeExists("cartItems", "totalPrice", "title", "content"));

        clearAuthentication();
    }

    @Test
    void viewCart_unauthenticatedUser_shouldRedirectToLogin() throws Exception {
        mockMvc.perform(get("/cart"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    // ---------- POST /cart/add/{id} ----------
    @Test
    void addToCart_success() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setRole(Role.USER);
        authenticateUser(user);

        CourseDto courseDto = new CourseDto();
        courseDto.setId(1L);
        when(courseService.getCourseDtoById(1L)).thenReturn(courseDto);
        doNothing().when(cartService).addCourseToCart(any(User.class), any(CourseDto.class));

        mockMvc.perform(post("/cart/add/1")
                        .header("Referer", "/courses/1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/courses/1"))
                .andExpect(flash().attribute("successMessage", "Курс добавлен в корзину"));

        clearAuthentication();
    }

    @Test
    void addToCart_courseNotFound() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setRole(Role.USER);
        authenticateUser(user);

        when(courseService.getCourseDtoById(99L)).thenThrow(new IllegalArgumentException());

        mockMvc.perform(post("/cart/add/99")
                        .header("Referer", "/courses/99"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/courses/99"))
                .andExpect(flash().attribute("errorMessage", "Курс не найден"));

        clearAuthentication();
    }

    @Test
    void addToCart_alreadyInCart() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setRole(Role.USER);
        authenticateUser(user);

        CourseDto courseDto = new CourseDto();
        courseDto.setId(1L);
        when(courseService.getCourseDtoById(1L)).thenReturn(courseDto);
        doThrow(new IllegalStateException("Курс уже добавлен в корзину")).when(cartService).addCourseToCart(any(User.class), any(CourseDto.class));

        mockMvc.perform(post("/cart/add/1")
                        .header("Referer", "/courses/1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/courses/1"))
                .andExpect(flash().attribute("errorMessage", "Курс уже добавлен в корзину"));

        clearAuthentication();
    }

    @Test
    void addToCart_unauthenticatedUser_shouldRedirectToLogin() throws Exception {
        mockMvc.perform(post("/cart/add/1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    // ---------- POST /cart/remove/{cartItemId} ----------
    @Test
    void removeCartItem_success() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setRole(Role.USER);
        authenticateUser(user);

        doNothing().when(cartService).removeCourseFromCart(any(User.class), eq(5L));

        mockMvc.perform(post("/cart/remove/5"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart"));

        clearAuthentication();
    }

    @Test
    void removeCartItem_unauthenticatedUser_shouldRedirectToLogin() throws Exception {
        mockMvc.perform(post("/cart/remove/1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    // ---------- POST /cart/clear ----------
    @Test
    void clearCart_success() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setRole(Role.USER);
        authenticateUser(user);

        doNothing().when(cartService).clearCart(any(User.class));

        mockMvc.perform(post("/cart/clear"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart"));

        clearAuthentication();
    }

    @Test
    void clearCart_unauthenticatedUser_shouldRedirectToLogin() throws Exception {
        mockMvc.perform(post("/cart/clear"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    // ---------- GET /cart/count ----------
    @Test
    void getCartCount_authenticatedUser() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setRole(Role.USER);
        authenticateUser(user);

        when(cartService.getCartItemCount(any(User.class))).thenReturn(3);

        mockMvc.perform(get("/cart/count"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"count\":3}"));

        clearAuthentication();
    }

    @Test
    void getCartCount_unauthenticatedUser_shouldReturnZero() throws Exception {
        mockMvc.perform(get("/cart/count"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"count\":0}"));
    }
}