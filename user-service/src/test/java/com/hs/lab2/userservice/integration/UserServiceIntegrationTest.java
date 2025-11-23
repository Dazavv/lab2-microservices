package com.hs.lab2.userservice.integration;

import com.hs.lab2.userservice.dto.UserDto;
import com.hs.lab2.userservice.repository.UserRepository;
import com.hs.lab2.userservice.requests.CreateUserRequest;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@Testcontainers
@ActiveProfiles("test")
class UserServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:15-alpine")).withDatabaseName("test_users_db").withUsername("test").withPassword("test").waitingFor(Wait.forListeningPort());
    @Autowired
    private WebTestClient webTestClient;
    @Autowired
    private UserRepository userRepository;
    private CreateUserRequest createRequest;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> postgres.getJdbcUrl());
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        registry.add("spring.flyway.enabled", () -> false);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.show-sql", () -> false);
    }

    @BeforeEach
    void setUp() {
        createRequest = new CreateUserRequest("integrationuser", "Integration", "Test");

        userRepository.deleteAll();
    }

    @Test
    void testCreateAndGetUser() {
        UserDto createdUser = webTestClient.post().uri("/api/v1/user").bodyValue(createRequest).exchange().expectStatus().isCreated().expectBody(UserDto.class).returnResult().getResponseBody();

        Assertions.assertThat(createdUser).isNotNull();
        Assertions.assertThat(createdUser.username()).isEqualTo("integrationuser");
        Assertions.assertThat(createdUser.name()).isEqualTo("Integration");

        webTestClient.get().uri("/api/v1/user/{id}", createdUser.id()).exchange().expectStatus().isOk().expectBody(UserDto.class).value(user -> {
            Assertions.assertThat(user.id()).isEqualTo(createdUser.id());
            Assertions.assertThat(user.username()).isEqualTo("integrationuser");
        });
    }

    @Test
    void testGetAllUsers() {
        CreateUserRequest request1 = new CreateUserRequest("user1", "Name1", "Surname1");
        CreateUserRequest request2 = new CreateUserRequest("user2", "Name2", "Surname2");

        webTestClient.post().uri("/api/v1/user").bodyValue(request1).exchange().expectStatus().isCreated();
        webTestClient.post().uri("/api/v1/user").bodyValue(request2).exchange().expectStatus().isCreated();
        webTestClient.get().uri("/api/v1/user?page=0&size=10").exchange().expectStatus().isOk().expectBody().jsonPath("$.content.length()").value(length -> Assertions.assertThat((Integer) length).isGreaterThanOrEqualTo(2));
    }

    @Test
    void testCreateDuplicateUser() {
        webTestClient.post().uri("/api/v1/user").bodyValue(createRequest).exchange().expectStatus().isCreated();
        webTestClient.post().uri("/api/v1/user").bodyValue(createRequest).exchange().expectStatus().isEqualTo(HttpStatus.CONFLICT).expectBody().jsonPath("$.message").isEqualTo("user with username = integrationuser already exists");
    }

    @Test
    void testGetUserByUsername() {
        UserDto createdUser = webTestClient.post().uri("/api/v1/user").bodyValue(createRequest).exchange().expectStatus().isCreated().expectBody(UserDto.class).returnResult().getResponseBody();

        Assertions.assertThat(createdUser).isNotNull();

        webTestClient.get().uri("/api/v1/user/by-username/{username}", createdUser.username()).exchange().expectStatus().isOk().expectBody(UserDto.class).value(user -> Assertions.assertThat(user.username()).isEqualTo(createdUser.username()));
    }

    @Test
    void testSearchUsersByUsername() {
        CreateUserRequest request1 = new CreateUserRequest("testuser1", "Name1", "Surname1");
        CreateUserRequest request2 = new CreateUserRequest("testuser2", "Name2", "Surname2");

        webTestClient.post().uri("/api/v1/user").bodyValue(request1).exchange().expectStatus().isCreated();
        webTestClient.post().uri("/api/v1/user").bodyValue(request2).exchange().expectStatus().isCreated();
        webTestClient.get().uri("/api/v1/user/search?q=testuser&page=0&size=10").exchange().expectStatus().isOk().expectBody().jsonPath("$.content.length()").value(length -> Assertions.assertThat((Integer) length).isGreaterThanOrEqualTo(2));
    }

    @Test
    void testDeleteUser() {
        UserDto createdUser = webTestClient.post().uri("/api/v1/user").bodyValue(createRequest).exchange().expectStatus().isCreated().expectBody(UserDto.class).returnResult().getResponseBody();

        Assertions.assertThat(createdUser).isNotNull();

        webTestClient.delete().uri("/api/v1/user/id/{id}", createdUser.id()).exchange().expectStatus().isOk();
        webTestClient.get().uri("/api/v1/user/{id}", createdUser.id()).exchange().expectStatus().isNotFound();
    }

    @Test
    void testGetAllUsersWithPagination() {
        for (int i = 0; i < 5; i++) {
            CreateUserRequest request = new CreateUserRequest("user" + i, "Name" + i, "Surname" + i);
            webTestClient.post().uri("/api/v1/user").bodyValue(request).exchange().expectStatus().isCreated();
        }

        webTestClient.get().uri("/api/v1/user?page=0&size=2").exchange().expectStatus().isOk().expectBody().jsonPath("$.content.length()").isEqualTo(2).jsonPath("$.totalElements").value(total -> Assertions.assertThat(((Number) total).longValue()).isGreaterThanOrEqualTo(5));
    }

    @Test
    void testGetAllUsersWithSorting() {
        CreateUserRequest request1 = new CreateUserRequest("auser", "A", "A");
        CreateUserRequest request2 = new CreateUserRequest("zuser", "Z", "Z");

        webTestClient.post().uri("/api/v1/user").bodyValue(request1).exchange().expectStatus().isCreated();
        webTestClient.post().uri("/api/v1/user").bodyValue(request2).exchange().expectStatus().isCreated();
        webTestClient.get().uri("/api/v1/user?page=0&size=10&sort=username").exchange().expectStatus().isOk().expectBody().jsonPath("$.content[0].username").isEqualTo("auser");
    }
}

