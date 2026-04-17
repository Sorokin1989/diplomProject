package com.example.diplomproject.mapper;

import com.example.diplomproject.dto.UserDto;
import com.example.diplomproject.entity.*;
import com.example.diplomproject.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserMapperTest {

    @Mock
    private OrderMapper orderMapper;
    @Mock
    private CertificateMapper certificateMapper;
    @Mock
    private ReviewMapper reviewMapper;
    @Mock
    private CourseAccessMapper courseAccessMapper;

    @InjectMocks
    private UserMapper userMapper;

    private User user;
    private Order order;
    private Certificate certificate;
    private Review review;
    private CourseAccess courseAccess;
    private Cart cart;

    @BeforeEach
    void setUp() {
        order = new Order();
        order.setId(1L);

        certificate = new Certificate();
        certificate.setId(2L);

        review = new Review();
        review.setId(3L);

        courseAccess = new CourseAccess();
        courseAccess.setId(4L);

        cart = new Cart();
        cart.setId(5L);

        user = new User();
        user.setId(10L);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setRole(Role.USER);
        user.setRegistrationDate(LocalDateTime.of(2025, 1, 1, 12, 0));
        user.setBonusPoints(100);
        user.setOrders(List.of(order));
        user.setCertificates(List.of(certificate));
        user.setReviews(List.of(review));
        user.setCourseAccesses(List.of(courseAccess));
        user.setCart(cart);
    }

    @Test
    void toUserDto_shouldMapFullUser() {
        when(orderMapper.toOrderDTO(any())).thenReturn(null);
        when(certificateMapper.toCertificateDto(any())).thenReturn(null);
        when(reviewMapper.toReviewDTO(any())).thenReturn(null);
        when(courseAccessMapper.toCourseAccessDto(any())).thenReturn(null);

        UserDto dto = userMapper.toUserDto(user);

        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(10L);
        assertThat(dto.getUsername()).isEqualTo("testuser");
        assertThat(dto.getEmail()).isEqualTo("test@example.com");
        assertThat(dto.getRole()).isEqualTo("USER");
        assertThat(dto.getRegistrationDate()).isEqualTo(LocalDateTime.of(2025, 1, 1, 12, 0));
        assertThat(dto.getBonusPoints()).isEqualTo(100);
        assertThat(dto.getOrderDtos()).hasSize(1);
        assertThat(dto.getCertificateDtos()).hasSize(1);
        assertThat(dto.getReviewDtos()).hasSize(1);
        assertThat(dto.getCourseAccessDtos()).hasSize(1);
        assertThat(dto.getCartId()).isEqualTo(5L);
    }

    @Test
    void toUserDto_shouldHandleNullCart() {
        user.setCart(null);
        UserDto dto = userMapper.toUserDto(user);
        assertThat(dto.getCartId()).isNull();
    }

    @Test
    void toUserDto_shouldReturnNullForNullInput() {
        assertThat(userMapper.toUserDto(null)).isNull();
    }

    @Test
    void fromUserDtoToEntity_shouldMapBasicFields() {
        UserDto dto = new UserDto();
        dto.setId(20L);
        dto.setUsername("newuser");
        dto.setEmail("new@example.com");
        dto.setRole("ADMIN");
        dto.setRegistrationDate(LocalDateTime.now());
        dto.setBonusPoints(50);

        User entity = userMapper.fromUserDtoToEntity(dto);

        assertThat(entity).isNotNull();
        assertThat(entity.getId()).isEqualTo(20L);
        assertThat(entity.getUsername()).isEqualTo("newuser");
        assertThat(entity.getEmail()).isEqualTo("new@example.com");
        assertThat(entity.getRole()).isEqualTo(Role.ADMIN);
        assertThat(entity.getBonusPoints()).isEqualTo(50);
        // Пароль не маппится (это ожидаемо)
        assertThat(entity.getPassword()).isNull();
        // Коллекции не маппятся (это правильно)
        assertThat(entity.getOrders()).isEmpty();
    }
}