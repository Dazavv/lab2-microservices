package com.hs.lab2.groupeventservice.entity;

import com.hs.lab2.groupeventservice.enums.GroupEventStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "group_events")
public class GroupEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 30)
    private String name;

    @Size(max = 200)
    private String description;

    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;

    @ElementCollection
    @CollectionTable(name = "group_event_participants", joinColumns = @JoinColumn(name = "group_event_id"))
    @Column(name = "participant_id")
    private List<Long> participantIds;

    @Column(name = "owner_id")
    private Long ownerId;

    @Enumerated(EnumType.STRING)
    private GroupEventStatus status;
}
