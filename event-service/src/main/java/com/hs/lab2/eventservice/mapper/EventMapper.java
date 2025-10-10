package com.hs.lab2.eventservice.mapper;

import com.hs.lab2.eventservice.dto.responses.EventDto;
import com.hs.lab2.eventservice.entity.Event;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface EventMapper {
    EventDto toEventDto(Event Event);
    List<EventDto> toEventDtoList(List<Event> events);
}
