package com.hs.lab2.groupeventservice.integration;

import com.hs.lab2.groupeventservice.client.EventClient;
import com.hs.lab2.groupeventservice.client.UserClient;
import com.hs.lab2.groupeventservice.dto.requests.BookSlotRequest;
import com.hs.lab2.groupeventservice.dto.requests.CreateGroupEventRequest;
import com.hs.lab2.groupeventservice.dto.requests.RecommendSlotsRequest;
import com.hs.lab2.groupeventservice.dto.responses.EventDto;
import com.hs.lab2.groupeventservice.dto.responses.GroupEventDto;
import com.hs.lab2.groupeventservice.dto.responses.UserDto;
import com.hs.lab2.groupeventservice.enums.GroupEventStatus;
import com.hs.lab2.groupeventservice.repository.GroupEventRepository;
import org.assertj.core.api.Assertions;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@Testcontainers
@ActiveProfiles("test")
class GroupEventServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:15-alpine"))
            .withDatabaseName("test_group_events_db")
            .withUsername("test")
            .withPassword("test")
            .waitingFor(Wait.forListeningPort());
    @Autowired
    private WebTestClient webTestClient;
    @Autowired
    private GroupEventRepository groupEventRepository;
    @MockitoBean
    private UserClient userClient;
    @MockitoBean
    private EventClient eventClient;
    private CreateGroupEventRequest createRequest;
    private UserDto testOwner;
    private UserDto testParticipant1;
    private UserDto testParticipant2;

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
        testOwner = new UserDto(1L, "owner", "Owner", "Owner", null);
        testParticipant1 = new UserDto(2L, "participant1", "Participant", "One", null);
        testParticipant2 = new UserDto(3L, "participant2", "Participant", "Two", null);

        createRequest = new CreateGroupEventRequest(
                "Integration Test Group Event",
                "Test Description",
                List.of(2L, 3L),
                1L
        );

        when(userClient.getUserById(1L)).thenReturn(Mono.just(testOwner));
        when(userClient.getUserById(2L)).thenReturn(Mono.just(testParticipant1));
        when(userClient.getUserById(3L)).thenReturn(Mono.just(testParticipant2));

        when(eventClient.getBusyEventsForUsersBetweenDates(anyList(), anyString(), anyString()))
                .thenReturn(Flux.empty());

        groupEventRepository.deleteAll();
    }

    @Test
    void testCreateAndGetGroupEvent() {
        GroupEventDto createdEvent = webTestClient.post()
                .uri("/api/v1/groupEvent")
                .bodyValue(createRequest)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(GroupEventDto.class)
                .returnResult()
                .getResponseBody();

        Assertions.assertThat(createdEvent).isNotNull();
        Assertions.assertThat(createdEvent.name()).isEqualTo("Integration Test Group Event");
        Assertions.assertThat(createdEvent.ownerId()).isEqualTo(1L);
        Assertions.assertThat(createdEvent.participantIds().size()).isEqualTo(2);
        Assertions.assertThat(createdEvent.status()).isEqualTo(GroupEventStatus.PENDING);

        webTestClient.get()
                .uri("/api/v1/groupEvent/{id}", createdEvent.id())
                .exchange()
                .expectStatus().isOk()
                .expectBody(GroupEventDto.class)
                .value(event -> {
                    Assertions.assertThat(event.id()).isEqualTo(createdEvent.id());
                    Assertions.assertThat(event.name()).isEqualTo("Integration Test Group Event");
                });
    }

    @Test
    void testGetAllGroupEvents() {
        CreateGroupEventRequest request2 = new CreateGroupEventRequest(
                "Event 2", "Description", List.of(2L), 1L);

        webTestClient.post()
                .uri("/api/v1/groupEvent")
                .bodyValue(createRequest)
                .exchange()
                .expectStatus().isCreated();

        webTestClient.post()
                .uri("/api/v1/groupEvent")
                .bodyValue(request2)
                .exchange()
                .expectStatus().isCreated();

        webTestClient.get()
                .uri("/api/v1/groupEvent")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(GroupEventDto.class)
                .value(events -> Assertions.assertThat(events.size()).isGreaterThanOrEqualTo(2));
    }

    @Test
    void testDeleteGroupEvent() {
        GroupEventDto createdEvent = webTestClient.post()
                .uri("/api/v1/groupEvent")
                .bodyValue(createRequest)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(GroupEventDto.class)
                .returnResult()
                .getResponseBody();

        Assertions.assertThat(createdEvent).isNotNull();

        webTestClient.delete()
                .uri("/api/v1/groupEvent/{id}", createdEvent.id())
                .exchange()
                .expectStatus().isOk();

        webTestClient.get()
                .uri("/api/v1/groupEvent/{id}", createdEvent.id())
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void testRecommendSlots() {
        GroupEventDto createdEvent = webTestClient.post()
                .uri("/api/v1/groupEvent")
                .bodyValue(createRequest)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(GroupEventDto.class)
                .returnResult()
                .getResponseBody();

        Assertions.assertThat(createdEvent).isNotNull();

        RecommendSlotsRequest recommendRequest = new RecommendSlotsRequest(
                LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(7),
                Duration.ofHours(1),
                createdEvent.id()
        );

        webTestClient.post()
                .uri("/api/v1/groupEvent/recommend")
                .bodyValue(recommendRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(GroupEventDto.class);
    }

    @Test
    void testBookSlot() {
        GroupEventDto createdEvent = webTestClient.post()
                .uri("/api/v1/groupEvent")
                .bodyValue(createRequest)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(GroupEventDto.class)
                .returnResult()
                .getResponseBody();

        Assertions.assertThat(createdEvent).isNotNull();

        BookSlotRequest bookRequest = new BookSlotRequest(
                createdEvent.id(),
                LocalDate.now().plusDays(1),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0)
        );

        webTestClient.post()
                .uri("/api/v1/groupEvent/book")
                .bodyValue(bookRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(GroupEventDto.class)
                .value(event -> {
                    Assertions.assertThat(event.status()).isEqualTo(GroupEventStatus.CONFIRMED);
                    Assertions.assertThat(event.date()).isNotNull();
                    Assertions.assertThat(event.startTime()).isNotNull();
                    Assertions.assertThat(event.endTime()).isNotNull();
                });
    }

    @Test
    void testCreateGroupEventWithInvalidOwner() {
        when(userClient.getUserById(999L)).thenReturn(Mono.error(new RuntimeException("User not found")));

        CreateGroupEventRequest invalidRequest = new CreateGroupEventRequest(
                "Invalid Event", "Description", List.of(2L), 999L);

        webTestClient.post()
                .uri("/api/v1/groupEvent")
                .bodyValue(invalidRequest)
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    void testRecommendSlotsWithBusyUsers() {
        // Create group event
        GroupEventDto createdEvent = webTestClient.post()
                .uri("/api/v1/groupEvent")
                .bodyValue(createRequest)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(GroupEventDto.class)
                .returnResult()
                .getResponseBody();

        Assertions.assertThat(createdEvent).isNotNull();

        LocalDate startDate = LocalDate.now().plusDays(1);
        LocalDate endDate = LocalDate.now().plusDays(7);

        EventDto busyEvent = new EventDto(
                1L, "Busy Event", null, startDate,
                LocalTime.of(9, 0), LocalTime.of(18, 0), 2L);

        when(eventClient.getBusyEventsForUsersBetweenDates(anyList(), anyString(), anyString()))
                .thenReturn(Flux.just(busyEvent));

        RecommendSlotsRequest recommendRequest = new RecommendSlotsRequest(
                startDate,
                endDate,
                Duration.ofHours(1),
                createdEvent.id()
        );

        webTestClient.post()
                .uri("/api/v1/groupEvent/recommend")
                .bodyValue(recommendRequest)
                .exchange()
                .expectStatus().isOk();
    }
}

