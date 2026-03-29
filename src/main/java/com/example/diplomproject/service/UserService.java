package com.example.diplomproject.service;

import com.example.diplomproject.dto.RegistrationDto;
import com.example.diplomproject.dto.UserDto;
import com.example.diplomproject.entity.User;
import com.example.diplomproject.enums.Role;
import com.example.diplomproject.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserDetailsService userDetailsService;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       UserDetailsService userDetailsService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userDetailsService = userDetailsService;
    }

    // === GETTERS ===

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Пользователь не найден"));
    }

    public User findByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email не может быть пустым");
        }
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new NoSuchElementException("Пользователя с таким email нет"));
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("Пользователь не найден"));
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Page<User> getUsersByPages(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    // === CREATE / REGISTER ===

    @Transactional
    public User createUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("Пользователь не может быть null");
        }
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("Имя пользователя не может быть пустым");
        }
        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Email не может быть пустым");
        }
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Пользователь с таким именем уже существует");
        }
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Пользователь с таким email уже существует");
        }
        if (user.getPassword() == null || user.getPassword().isEmpty()) {
            throw new IllegalArgumentException("Пароль не может быть пустым");
        }

        if (user.getRole() == null) {
            user.setRole(Role.USER);
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        return userRepository.save(user);
    }

    @Transactional
    public User registerNewUser(RegistrationDto dto) {
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new IllegalArgumentException("Пароли не совпадают");
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setPassword(dto.getPassword()); // ещё не закодирован
        user.setRole(Role.USER);

        return createUser(user);
    }

    // === UPDATE ===

    @Transactional
    public User updateUser(Long id, User userDetails) {
        if (userDetails == null) {
            throw new IllegalArgumentException("Данные для обновления не могут быть null");
        }
        User user = getUserById(id);

        if (userDetails.getUsername() != null && !userDetails.getUsername().trim().isEmpty()) {
            user.setUsername(userDetails.getUsername().trim());
        }
        if (userDetails.getEmail() != null && !userDetails.getEmail().trim().isEmpty()) {
            String newEmail = userDetails.getEmail().trim();
            if (!newEmail.equals(user.getEmail())) {
                if (userRepository.findByEmail(newEmail).isPresent()) {
                    throw new IllegalArgumentException("Email уже используется");
                }
                user.setEmail(newEmail);
            }
        }
        // обновление пароля при необходимости
        // if (userDetails.getPassword() != null && !userDetails.getPassword().isEmpty()) {
        //     user.setPassword(passwordEncoder.encode(userDetails.getPassword()));
        // }

        return userRepository.save(user);
    }

    @Transactional
    public boolean updateUserFromDto(Long id, UserDto dto) {
        User user = getUserById(id);
        boolean updated = false;
        String oldUsername = user.getUsername();

        // Изменение имени
        if (dto.getUsername() != null && !dto.getUsername().trim().isEmpty()) {
            String newUsername = dto.getUsername().trim();
            if (!newUsername.equals(user.getUsername())) {
                if (userRepository.findByUsername(newUsername).isPresent()) {
                    throw new IllegalArgumentException("Такое имя занято! Введите другое.");
                }
                user.setUsername(newUsername);
                updated = true;
            }
        }

        // Изменение email
        if (dto.getEmail() != null && !dto.getEmail().trim().isEmpty()) {
            String newEmail = dto.getEmail().trim();
            if (!newEmail.equals(user.getEmail())) {
                if (userRepository.findByEmail(newEmail).isPresent()) {
                    throw new IllegalArgumentException("Этот Email зарегистрировался ранее.");
                }
                user.setEmail(newEmail);
                updated = true;
            }
        }

        // Изменение роли (если разрешено)
        if (dto.getRole() != null && !dto.getRole().isBlank()) {
            try {
                Role newRole = Role.valueOf(dto.getRole().toUpperCase());
                if (!newRole.equals(user.getRole())) {
                    user.setRole(newRole);
                    updated = true;
                }
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException("Невалидная РОЛЬ");
            }
        }

        if (updated) {
            User saved = userRepository.save(user);
            System.out.println("Сохранённый пользователь: id=" + saved.getId() +
                    ", username=" + saved.getUsername() +
                    ", email=" + saved.getEmail() +
                    ", role=" + saved.getRole());


            // Если имя изменилось – обновляем аутентификацию в SecurityContext
            if (!oldUsername.equals(user.getUsername())) {
                updateAuthentication(oldUsername, user.getUsername());
            }
            log.info("User {} updated: new data: {}", id, dto);
        } else {
            log.info("User {} update attempted with no changes", id);
        }

        return updated;
    }

    // === DELETE ===

    @Transactional
    public void deleteUser(Long id) {
        User user = getUserById(id);
        userRepository.delete(user);
        System.out.println("Deleting user: " + user.getUsername());
    }

    // === ВСПОМОГАТЕЛЬНЫЙ МЕТОД ДЛЯ ОБНОВЛЕНИЯ АУТЕНТИФИКАЦИИ ===

    private void updateAuthentication(String oldUsername, String newUsername) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName().equals(oldUsername)) {
            UserDetails updatedUser = userDetailsService.loadUserByUsername(newUsername);
            Authentication newAuth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                    updatedUser, auth.getCredentials(), updatedUser.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(newAuth);
        }
    }
    public boolean existsByUsername(String username) {
        return userRepository.findByUsername(username).isPresent();
    }

    public boolean existsByEmail(String email) {
        return userRepository.findByEmail(email).isPresent();
    }
}