package com.example.diplomproject.controller;

import com.example.diplomproject.entity.User;
import com.example.diplomproject.enums.Role;
import com.example.diplomproject.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminInitializerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminInitializer adminInitializer;

    @BeforeEach
    void setUp() {
        // Устанавливаем значения полей, которые обычно приходят из @Value
        ReflectionTestUtils.setField(adminInitializer, "adminUsername", "admin");
        ReflectionTestUtils.setField(adminInitializer, "adminPassword", "admin123");
        ReflectionTestUtils.setField(adminInitializer, "adminEmail", "admin@example.com");
    }

    @Test
    void run_shouldCreateAdminWhenNotExists() throws Exception {
        // given
        when(userRepository.findByUsername("admin")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("admin123")).thenReturn("encodedPassword");

        // when
        adminInitializer.run();

        // then
        verify(userRepository).findByUsername("admin");
        verify(passwordEncoder).encode("admin123");
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void run_shouldNotCreateAdminWhenAlreadyExists() throws Exception {
        // given
        User existingAdmin = new User();
        existingAdmin.setUsername("admin");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(existingAdmin));

        // when
        adminInitializer.run();

        // then
        verify(userRepository).findByUsername("admin");
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void run_shouldCreateAdminWithCorrectFields() throws Exception {
        // given
        when(userRepository.findByUsername("admin")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("admin123")).thenReturn("encodedPassword");

        // when
        adminInitializer.run();

        // then
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User savedUser = captor.getValue();

        assertThat(savedUser.getUsername()).isEqualTo("admin");
        assertThat(savedUser.getPassword()).isEqualTo("encodedPassword");
        assertThat(savedUser.getEmail()).isEqualTo("admin@example.com");
        assertThat(savedUser.getRole()).isEqualTo(Role.ADMIN);
    }
}