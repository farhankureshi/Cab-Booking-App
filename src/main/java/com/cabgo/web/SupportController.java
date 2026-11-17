package com.cabgo.web;

import com.cabgo.dto.Dtos.SupportRequest;
import com.cabgo.model.Role;
import com.cabgo.model.SupportTicket;
import com.cabgo.model.User;
import com.cabgo.repo.SupportTicketRepository;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/support")
public class SupportController {
    private final SupportTicketRepository tickets;

    public SupportController(SupportTicketRepository tickets) {
        this.tickets = tickets;
    }

    @PostMapping
    public SupportTicket create(@AuthenticationPrincipal User u, @Valid @RequestBody SupportRequest r) {
        SupportTicket t = new SupportTicket();
        t.setUser(u);
        t.setSubject(r.subject());
        t.setMessage(r.message());
        return tickets.save(t);
    }

    @GetMapping
    public List<SupportTicket> list(@AuthenticationPrincipal User u) {
        return u.getRole() == Role.ADMIN
                ? tickets.findAll(Sort.by(Sort.Direction.DESC, "id"))
                : tickets.findByUserOrderByIdDesc(u);
    }
}
