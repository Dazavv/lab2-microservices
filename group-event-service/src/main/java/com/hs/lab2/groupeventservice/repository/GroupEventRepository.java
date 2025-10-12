package com.hs.lab2.groupeventservice.repository;


import com.hs.lab2.groupeventservice.entity.GroupEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GroupEventRepository extends JpaRepository<GroupEvent, Long> {
    @Query("""
    SELECT g FROM GroupEvent g
    LEFT JOIN FETCH g.participantIds
    """)
    List<GroupEvent> findAllWithParticipants();

    @Query("""
    SELECT g FROM GroupEvent g
    LEFT JOIN FETCH g.participantIds
    WHERE g.id = :id
    """)
    Optional<GroupEvent> findByIdWithParticipants(@Param("id") Long id);


}
