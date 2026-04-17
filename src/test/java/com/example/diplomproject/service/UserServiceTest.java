package com.example.diplomproject.service;

import com.example.diplomproject.dto.RegistrationDto;
import com.example.diplomproject.dto.UserDto;
import com.example.diplomproject.entity.User;
import com.example.diplomproject.enums.Role;
import com.example.diplomproject.mapper.UserMapper;
import com.example.diplomproject.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UserDetailsService userDetailsService;
    @Mock
    private UserMapper userMapper;
    @Mock
    private Authentication authentication;

    @InjectMocks
    private UserService userService;

    private User user;
    private UserDto userDto;
    private RegistrationDto registrationDto;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPassword("encodedPassword");
        user.setRole(Role.USER);
        user.setBonusPoints(0);

        userDto = new UserDto();
        userDto.setId(1L);
        userDto.setUsername("testuser");
        userDto.setEmail("test@example.com");
        userDto.setRole("USER");

        registrationDto = new RegistrationDto();
        registrationDto.setUsername("newuser");
        registrationDto.setEmail("new@example.com");
        registrationDto.setPassword("password");
        registrationDto.setConfirmPassword("password");
    }

    // ========== GETTERS ==========
    @Test
    void getUserById_shouldReturnUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        User found = userService.getUserById(1L);
        assertThat(found).isEqualTo(user);
    }

    @Test
    void getUserById_shouldThrowWhenNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> userService.getUserById(99L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("Пользователь не найден");
    }

    @Test
    void findByEmail_shouldReturnUser() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        User found = userService.findByEmail("test@example.com");
        assertThat(found).isEqualTo(user);
    }

    @Test
    void findByEmail_shouldThrowWhenNotFound() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> userService.findByEmail("unknown@example.com"))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void findByUsername_shouldReturnUser() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        User found = userService.findByUsername("testuser");
        assertThat(found).isEqualTo(user);
    }

    @Test
    void getAllUsers_shouldReturnList() {
        when(userRepository.findAll()).thenReturn(List.of(user));
        List<User> result = userService.getAllUsers();
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(user);
    }

    @Test
    void getUsersByPages_shouldReturnPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> page = new PageImpl<>(List.of(user), pageable, 1);
        when(userRepository.findAll(pageable)).thenReturn(page);
        Page<User> result = userService.getUsersByPages(pageable);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0)).isEqualTo(user);
    }

    // ========== CREATE / REGISTER ==========
    @Test
    void createUser_shouldSaveValidUser() {
        User newUser = new User();
        newUser.setUsername("newuser");
        newUser.setEmail("new@example.com");
        newUser.setPassword("rawPassword");
        newUser.setRole(Role.USER);

        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("rawPassword")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(newUser);

        User saved = userService.createUser(newUser);

        assertThat(saved.getPassword()).isEqualTo("encodedPassword");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUser_shouldThrowWhenUsernameExists() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        assertThatThrownBy(() -> userService.createUser(user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Пользователь с таким именем уже существует");
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerNewUser_shouldRegisterValidUser() {
        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User registered = userService.registerNewUser(registrationDto);

        assertThat(registered.getUsername()).isEqualTo("newuser");
        assertThat(registered.getEmail()).isEqualTo("new@example.com");
        assertThat(registered.getRole()).isEqualTo(Role.USER);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerNewUser_shouldThrowWhenPasswordsDoNotMatch() {
        registrationDto.setConfirmPassword("different");
        assertThatThrownBy(() -> userService.registerNewUser(registrationDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Пароли не совпадают");
        verify(userRepository, never()).save(any());
    }

    // ========== UPDATE ==========
    @Test
    void updateUserFromDto_shouldUpdateUsernameAndEmail() {
        UserDto updateDto = new UserDto();
        updateDto.setUsername("newusername");
        updateDto.setEmail("newemail@example.com");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.findByUsername("newusername")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("newemail@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(user);
        // Мокаем SecurityContext для updateAuthentication (чтобы не было NPE)
        SecurityContextHolder.getContext().setAuthentication(authentication);
        when(authentication.getName()).thenReturn("testuser");
        when(userDetailsService.loadUserByUsername("newusername")).thenReturn(mock(UserDetails.class));

        boolean updated = userService.updateUserFromDto(1L, updateDto);

        assertThat(updated).isTrue();
        assertThat(user.getUsername()).isEqualTo("newusername");
        assertThat(user.getEmail()).isEqualTo("newemail@example.com");
        verify(userRepository).save(user);
        SecurityContextHolder.clearContext();
    }

    @Test
    void updateUserFromDto_shouldThrowWhenUsernameTaken() {
        UserDto updateDto = new UserDto();
        updateDto.setUsername("taken");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.findByUsername("taken")).thenReturn(Optional.of(new User()));

        assertThatThrownBy(() -> userService.updateUserFromDto(1L, updateDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Такое имя занято! Введите другое.");
        verify(userRepository, never()).save(any());
    }

    // ========== DELETE ==========
    @Test
    void deleteUser_shouldDelete() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        userService.deleteUser(1L);
        verify(userRepository).delete(user);
    }

    // ========== EXISTS ==========
    @Test
    void existsByUsername_shouldReturnTrue() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        boolean exists = userService.existsByUsername("testuser");
        assertThat(exists).isTrue();
    }

    @Test
    void existsByEmail_shouldReturnFalse() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());
        boolean exists = userService.existsByEmail("unknown@example.com");
        assertThat(exists).isFalse();
    }

    // ========== DTO METHODS ==========
    @Test
    void getUserDtoById_shouldReturnDto() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userMapper.toUserDto(user)).thenReturn(userDto);
        UserDto result = userService.getUserDtoById(1L);
        assertThat(result).isEqualTo(userDto);
    }

    @Test
    void getUserDtoByUsername_shouldReturnDto() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(userMapper.toUserDto(user)).thenReturn(userDto);
        UserDto result = userService.getUserDtoByUsername("testuser");
        assertThat(result).isEqualTo(userDto);
    }
}