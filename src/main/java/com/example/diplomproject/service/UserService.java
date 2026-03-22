package com.example.diplomproject.service;

import com.example.diplomproject.entity.User;
import com.example.diplomproject.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    /**
     * Получение пользователя по ID.
     * @param id идентификатор
     * @return найденный пользователь
     * @throws NoSuchElementException если пользователь не найден
     */
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Пользователь не найден"));
    }

    /**
     * Получение пользователя по email.
     * @param email email пользователя
     * @return найденный пользователь
     * @throws NoSuchElementException если пользователь с таким email не найден
     */
    public User findByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email не может быть пустым");
        }
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new NoSuchElementException("Пользователя с таким email нет"));
    }

    /**
     * Создание нового пользователя.
     * @param user объект пользователя (должен содержать username, email)
     * @return сохранённый пользователь
     */
    @Transactional
    public User createUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("Пользователь не может быть null");
        }
        // Проверка обязательных полей
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("Имя пользователя не может быть пустым");
        }
        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Email не может быть пустым");
        }
        // Проверка уникальности email
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Пользователь с таким email уже существует");
        }
        // Если есть поле password, следует проверить его наличие и захешировать перед сохранением
        // if (user.getPassword() == null || user.getPassword().isEmpty()) {
        //     throw new IllegalArgumentException("Пароль не может быть пустым");
        // }
        // user.setPassword(passwordEncoder.encode(user.getPassword()));

        return userRepository.save(user);
    }

    /**
     * Обновление данных пользователя.
     * @param id          идентификатор обновляемого пользователя
     * @param userDetails объект с новыми данными (не может быть null)
     * @return обновлённый пользователь
     */
    @Transactional
    public User updateUser(Long id, User userDetails) {
        if (userDetails == null) {
            throw new IllegalArgumentException("Данные для обновления не могут быть null");
        }
        User user = getUserById(id);

        // Обновление имени
        if (userDetails.getUsername() != null && !userDetails.getUsername().trim().isEmpty()) {
            user.setUsername(userDetails.getUsername().trim());
        }

        // Обновление email с проверкой уникальности
        if (userDetails.getEmail() != null && !userDetails.getEmail().trim().isEmpty()) {
            String newEmail = userDetails.getEmail().trim();
            if (!newEmail.equals(user.getEmail())) {
                if (userRepository.findByEmail(newEmail).isPresent()) {
                    throw new IllegalArgumentException("Email уже используется");
                }
                user.setEmail(newEmail);
            }
        }

        // Если есть поле password, можно добавить логику обновления
        // if (userDetails.getPassword() != null && !userDetails.getPassword().isEmpty()) {
        //     user.setPassword(passwordEncoder.encode(userDetails.getPassword()));
        // }

        // Сохраняем изменения (явный save не обязателен, так как объект уже managed, но оставим для наглядности)
        return userRepository.save(user);
    }

    /**
     * Удаление пользователя.
     * @param id идентификатор пользователя
     */
    @Transactional
    public void deleteUser(Long id) {
        User user = getUserById(id);
        // Перед удалением можно проверить, нет ли связанных данных (заказов, отзывов),
        // чтобы избежать ошибок внешнего ключа. Если каскадные связи настроены, они обработаются автоматически.
        userRepository.delete(user);
    }

    /**
     * Получение всех пользователей.
     * @return список всех пользователей
     */
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}