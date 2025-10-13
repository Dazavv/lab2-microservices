package com.hs.lab1.service;

import com.hs.lab1.entity.User;
import com.hs.lab1.exceptions.UserNotFoundException;
import com.hs.lab1.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User addUser(String username, String name, String surname) {
        boolean exists = userRepository.existsByUsername(username);
        if (exists)
            throw new IllegalArgumentException("user with username = " + username + " already exists"); //TODO мб пользовательский эксепшен сделать

        User user = new User();
        user.setUsername(username);
        user.setName(name);
        user.setSurname(surname);

        return userRepository.save(user);
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User with id = " + id + " not found"));
    }

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("user with username = " + username + " does not exist"));
    }

    public void deleteUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User with id = " + id + " not found"));
        userRepository.delete(user);
    }

    public Page<User> searchByUsername(String query, Pageable pageable) {
        return userRepository.findByUsernameContainingIgnoreCase(query, pageable);
    }
}
