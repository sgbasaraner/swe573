package io.github.sgbasaraner.funxchange.controller;

import io.github.sgbasaraner.funxchange.model.EventDTO;
import io.github.sgbasaraner.funxchange.model.NewEventDTO;
import io.github.sgbasaraner.funxchange.model.UserDTO;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
public class EventController {
    @GetMapping("/events/{eventId}")
    EventDTO fetchEvent(Principal principal, @PathVariable String eventId) {
        return null;
    }

    @GetMapping("/events/feed")
    List<EventDTO> fetchFeed(Principal principal, @RequestParam int offset, @RequestParam int limit) {
        return null;
    }

    @GetMapping("/user/{userId}/events")
    List<EventDTO> fetchEventsOfUser(Principal principal, @RequestParam int offset, @RequestParam int limit, @PathVariable String userId) {
        return null;
    }

    @GetMapping("/events/{eventId}/participants")
    List<UserDTO> fetchParticipantsOfEvent(Principal principal, @RequestParam int offset, @RequestParam int limit, @PathVariable String eventId) {
        return null;
    }

    @PostMapping("/events")
    EventDTO createEvent(Principal principal, @RequestBody NewEventDTO params) {
        return null;
    }
}
