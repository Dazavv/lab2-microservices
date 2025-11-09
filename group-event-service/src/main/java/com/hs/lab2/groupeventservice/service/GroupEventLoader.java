package com.hs.lab2.groupeventservice.service;

import com.hs.lab2.groupeventservice.exceptions.EventNotFoundException;
import com.hs.lab2.groupeventservice.repository.GroupEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GroupEventLoader {
    private final GroupEventRepository repo;

    @Transactional(readOnly = true)
    public List<Long> loadParticipantIds(Long id) {
        var ge = repo.findByIdWithParticipants(id)
                .orElseThrow(() -> new EventNotFoundException("GroupEvent not found: " + id));
        return List.copyOf(ge.getParticipantIds());
    }
}
