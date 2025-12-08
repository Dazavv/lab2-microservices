package com.hs.lab2.eventservice.integration;

import com.hs.lab2.eventservice.client.UserClient;
import com.hs.lab2.eventservice.dto.requests.CreateEventRequest;
import com.hs.lab2.eventservice.dto.responses.EventDto;
import com.hs.lab2.eventservice.dto.responses.UserDto;
import com.hs.lab2.eventservice.entity.Event;
import com.hs.lab2.eventservice.repository.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@Testcontainers
@ActiveProfiles("test")
class EventServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:15-alpine"))
            .withDatabaseName("test_events_db")
            .withUsername("test")
            .withPassword("test")
            .waitingFor(Wait.forListeningPort());

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // JDBC for Flyway
        registry.add("spring.flyway.url", postgres::getJdbcUrl);
        registry.add("spring.flyway.user", postgres::getUsername);
        registry.add("spring.flyway.password", postgres::getPassword);

        registry.add("spring.r2dbc.url", () -> 
                String.format("r2dbc:postgresql://%s:%d/%s", 
                        postgres.getHost(), postgres.getMappedPort(5432), postgres.getDatabaseName()));
        registry.add("spring.r2dbc.username", postgres::getUsername);
        registry.add("spring.r2dbc.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> true);
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
    }

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private EventRepository eventRepository;

    @MockitoBean
    private UserClient userClient;

    private UserDto testUser;
    private CreateEventRequest createRequest;

    @BeforeEach
    void setUp() {
        testUser = new UserDto(1L, "testuser", "Test", "User", null);

        createRequest = new CreateEventRequest(
                "IntegrationTestEvent",
                "Test Description",
                LocalDate.now().plusDays(1),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                1L
        );

        when(userClient.getUserById(anyLong())).thenReturn(Mono.just(testUser));

        eventRepository.deleteAll().block();
    }

    @Test
    void testCreateAndGetEvent() {
        EventDto createdEvent = webTestClient.post()
                .uri("/api/v1/event")
                .bodyValue(createRequest)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(EventDto.class)
                .returnResult()
                .getResponseBody();

        org.assertj.core.api.Assertions.assertThat(createdEvent).isNotNull();
        org.assertj.core.api.Assertions.assertThat(createdEvent.name()).isEqualTo("IntegrationTestEvent");
        org.assertj.core.api.Assertions.assertThat(createdEvent.ownerId()).isEqualTo(1L);

        webTestClient.get()
                .uri("/api/v1/event/{id}", createdEvent.id())
                .exchange()
                .expectStatus().isOk()
                .expectBody(EventDto.class)
                .value(event -> {
                    org.assertj.core.api.Assertions.assertThat(event.id()).isEqualTo(createdEvent.id());
                    org.assertj.core.api.Assertions.assertThat(event.name()).isEqualTo("IntegrationTestEvent");
                });
    }

    @Test
    void testGetAllEvents() {
        CreateEventRequest request2 = new CreateEventRequest(
                "Event 2", "Description", LocalDate.now().plusDays(1),
                LocalTime.of(12, 0), LocalTime.of(13, 0), 1L);

        webTestClient.post()
                .uri("/api/v1/event")
                .bodyValue(createRequest)
                .exchange()
                .expectStatus().isCreated();

        webTestClient.post()
                .uri("/api/v1/event")
                .bodyValue(request2)
                .exchange()
                .expectStatus().isCreated();

        webTestClient.get()
                .uri("/api/v1/event")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(EventDto.class)
                .value(events -> org.assertj.core.api.Assertions.assertThat(events.size()).isGreaterThanOrEqualTo(2));
    }

    @Test
    void testCreateEventWithConflict() {
        webTestClient.post()
                .uri("/api/v1/event")
                .bodyValue(createRequest)
                .exchange()
                .expectStatus().isCreated();

        CreateEventRequest overlappingRequest = new CreateEventRequest(
                "Overlapping Event", "Description", createRequest.date(),
                LocalTime.of(10, 30), LocalTime.of(11, 30), 1L);

        webTestClient.post()
                .uri("/api/v1/event")
                .bodyValue(overlappingRequest)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void testDeleteEvent() {
        EventDto createdEvent = webTestClient.post()
                .uri("/api/v1/event")
                .bodyValue(createRequest)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(EventDto.class)
                .returnResult()
                .getResponseBody();

        org.assertj.core.api.Assertions.assertThat(createdEvent).isNotNull();

        webTestClient.delete()
                .uri("/api/v1/event/{id}", createdEvent.id())
                .exchange()
                .expectStatus().isNoContent();

        webTestClient.get()
                .uri("/api/v1/event/{id}", createdEvent.id())
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void testGetUserEvents() {
        webTestClient.post()
                .uri("/api/v1/event")
                .bodyValue(createRequest)
                .exchange()
                .expectStatus().isCreated();

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/v1/event/owner/{id}")
                        .queryParam("page", 0)
                        .queryParam("size", 10)
                        .build(1L))
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Event.class)
                .value(events -> org.assertj.core.api.Assertions.assertThat(events).isNotEmpty());
    }

    @Test
    void testGetBusyEventsForUsers() {
        webTestClient.post()
                .uri("/api/v1/event")
                .bodyValue(createRequest)
                .exchange()
                .expectStatus().isCreated();

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/v1/event/busy")
                        .queryParam("userIds", "1,2")
                        .queryParam("startDate", LocalDate.now().toString())
                        .queryParam("endDate", LocalDate.now().plusDays(7).toString())
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(EventDto.class);
    }

    @Test
    void testCreateEventWithInvalidTime() {
        CreateEventRequest invalidRequest = new CreateEventRequest(
                "Invalid Event", "Description", LocalDate.now().minusDays(1),
                LocalTime.of(10, 0), LocalTime.of(11, 0), 1L);

        webTestClient.post()
                .uri("/api/v1/event")
                .bodyValue(invalidRequest)
                .exchange()
                .expectStatus().isBadRequest();
    }
}

