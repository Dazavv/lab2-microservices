package com.hs.lab2.userservice.service;

import com.hs.lab2.userservice.entity.User;
import com.hs.lab2.userservice.exceptions.UserAlreadyExistsException;
import com.hs.lab2.userservice.exceptions.UserNotFoundException;
import com.hs.lab2.userservice.repository.UserRepository;
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

import java.util.List;
import java.util.Optional;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setName("Test");
        testUser.setSurname("User");
    }

    @Test
    void testGetAllUsers() {
        User user2 = new User();
        user2.setId(2L);

        Pageable pageable = PageRequest.of(0, 25);
        Page<User> userPage = new PageImpl<>(List.of(testUser, user2));

        when(userRepository.findAll(any(Pageable.class))).thenReturn(userPage);

        StepVerifier.create(userService.getAllUsers(pageable))
                .expectNextMatches(page -> page.getTotalElements() == 2)
                .verifyComplete();

        verify(userRepository).findAll(pageable);
    }

    @Test
    void testAddUser_Success() {
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        StepVerifier.create(userService.addUser("testuser", "Test", "User"))
                .expectNextMatches(user -> user.getUsername().equals("testuser"))
                .verifyComplete();

        verify(userRepository).save(any(User.class));
    }

    @Test
    void testAddUser_AlreadyExists() {
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        StepVerifier.create(userService.addUser("testuser", "Test", "User"))
                .expectError(UserAlreadyExistsException.class)
                .verify();

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testGetUserById_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        StepVerifier.create(userService.getUserById(1L))
                .expectNext(testUser)
                .verifyComplete();
    }

    @Test
    void testGetUserById_NotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        StepVerifier.create(userService.getUserById(1L))
                .expectError(UserNotFoundException.class)
                .verify();
    }

    @Test
    void testGetUserByUsername_Success() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        StepVerifier.create(userService.getUserByUsername("testuser"))
                .expectNext(testUser)
                .verifyComplete();
    }

    @Test
    void testGetUserByUsername_NotFound() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());

        StepVerifier.create(userService.getUserByUsername("testuser"))
                .expectError(UserNotFoundException.class)
                .verify();
    }

    @Test
    void testDeleteUserById_Success() {
        when(userRepository.existsById(1L)).thenReturn(true);
        doNothing().when(userRepository).deleteById(1L);

        StepVerifier.create(userService.deleteUserById(1L))
                .verifyComplete();

        verify(userRepository).deleteById(1L);
    }

    @Test
    void testDeleteUserById_NotFound() {
        when(userRepository.existsById(1L)).thenReturn(false);

        StepVerifier.create(userService.deleteUserById(1L))
                .expectError(UserNotFoundException.class)
                .verify();

        verify(userRepository, never()).deleteById(anyLong());
    }

    @Test
    void testSearchByUsername() {
        Pageable pageable = PageRequest.of(0, 25);
        Page<User> userPage = new PageImpl<>(List.of(testUser));

        when(userRepository.findByUsernameContainingIgnoreCase("test", pageable))
                .thenReturn(userPage);

        StepVerifier.create(userService.searchByUsername("test", pageable))
                .expectNextMatches(page -> page.getTotalElements() == 1)
                .verifyComplete();

        verify(userRepository).findByUsernameContainingIgnoreCase("test", pageable);
    }
}

