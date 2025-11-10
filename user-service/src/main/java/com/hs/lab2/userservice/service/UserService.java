package com.hs.lab2.userservice.service;

import com.hs.lab2.userservice.entity.User;
import com.hs.lab2.userservice.exceptions.UserNotFoundException;
import com.hs.lab2.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public Mono<Page<User>> getAllUsers(Pageable pageable) {
        return Mono.fromCallable(() -> userRepository.findAll(pageable))
                .subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<User> addUser(String username, String name, String surname) {
        return Mono.fromCallable(() -> {
            if (userRepository.existsByUsername(username)) {
                throw new IllegalArgumentException("user with username = " + username + " already exists");
            }

            User user = new User();
            user.setUsername(username);
            user.setName(name);
            user.setSurname(surname);

            return userRepository.save(user);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<User> getUserById(Long id) {
        return Mono.fromCallable(() -> userRepository.findById(id)
                        .orElseThrow(() -> new UserNotFoundException("User with id = " + id + " not found")))

                .subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<User> getUserByUsername(String username) {
        return Mono.fromCallable(() -> userRepository.findByUsername(username)
                        .orElseThrow(() -> new UserNotFoundException("User with username = " + username + " not found")))

                .subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<Void> deleteUserById(Long id) {
        return Mono.fromRunnable(() -> {
                    if (!userRepository.existsById(id))
                        throw new UserNotFoundException("User with id = " + id + " not found");
                    userRepository.deleteById(id);
                })
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    public Mono<Page<User>> searchByUsername(String query, Pageable pageable) {
        return Mono.fromCallable(() ->
                        userRepository.findByUsernameContainingIgnoreCase(query, pageable)
                )
                .subscribeOn(Schedulers.boundedElastic());
    }
}
