package com.cabgo.web;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/** WebSocket (STOMP) endpoints: live driver location + SOS broadcasting. */
@Controller
public class RealtimeController {

    @MessageMapping("/ride/{id}/location")
    @SendTo("/topic/ride/{id}")
    public Map<String, Object> location(@DestinationVariable Long id, Map<String, Object> payload) {
        Map<String, Object> out = new HashMap<>();
        out.put("type", "LOCATION");
        out.put("lat", payload.get("lat"));
        out.put("lng", payload.get("lng"));
        return out;
    }

    @MessageMapping("/sos")
    @SendTo("/topic/sos")
    public Map<String, Object> sos(Map<String, Object> payload) {
        Map<String, Object> out = new HashMap<>(payload);
        out.put("type", "SOS");
        out.put("at", Instant.now().toString());
        return out;
    }
}
