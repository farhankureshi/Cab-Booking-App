package com.cabgo.web;

import com.cabgo.dto.Dtos.BookRequest;
import com.cabgo.dto.Dtos.RatingRequest;
import com.cabgo.model.Ride;
import com.cabgo.model.RideStatus;
import com.cabgo.model.Role;
import com.cabgo.model.User;
import com.cabgo.service.ReceiptService;
import com.cabgo.service.RideService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/rides")
public class RideController {
    private final RideService service;
    private final ReceiptService receipts;

    public RideController(RideService service, ReceiptService receipts) {
        this.service = service;
        this.receipts = receipts;
    }

    @PostMapping("/estimate")
    public RideService.Quote estimate(@Valid @RequestBody BookRequest r) {
        return service.quote(r);
    }

    @PostMapping
    @PreAuthorize("hasRole('RIDER')")
    public Ride book(@AuthenticationPrincipal User u, @Valid @RequestBody BookRequest r) {
        return service.book(u, r);
    }

    @GetMapping("/my")
    public List<Ride> my(@AuthenticationPrincipal User u) {
        return service.history(u);
    }

    @GetMapping("/active")
    public ResponseEntity<Ride> active(@AuthenticationPrincipal User u) {
        Optional<Ride> r = service.active(u);
        if (r.isEmpty()) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(r.get());
    }

    @GetMapping("/open")
    @PreAuthorize("hasRole('DRIVER')")
    public List<Ride> open(@AuthenticationPrincipal User u) {
        return service.open(u);
    }

    @PostMapping("/{id}/accept")
    @PreAuthorize("hasRole('DRIVER')")
    public Ride accept(@AuthenticationPrincipal User u, @PathVariable Long id) {
        return service.accept(u, id);
    }

    @PostMapping("/{id}/status")
    @PreAuthorize("hasRole('DRIVER')")
    public Ride status(@AuthenticationPrincipal User u, @PathVariable Long id, @RequestParam RideStatus to) {
        return service.updateStatus(u, id, to);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('RIDER')")
    public Ride cancel(@AuthenticationPrincipal User u, @PathVariable Long id) {
        return service.cancel(u, id);
    }

    @PostMapping("/{id}/rating")
    @PreAuthorize("hasRole('RIDER')")
    public Map<String, Object> rate(@AuthenticationPrincipal User u, @PathVariable Long id,
                                    @Valid @RequestBody RatingRequest r) {
        service.rate(u, id, r);
        return Map.of("ok", true);
    }

    @GetMapping("/{id}/receipt")
    public ResponseEntity<byte[]> receipt(@AuthenticationPrincipal User u, @PathVariable Long id) {
        Ride r = service.get(id);
        boolean mine = u.getRole() == Role.ADMIN
                || r.getRider().getId().equals(u.getId())
                || (r.getDriver() != null && r.getDriver().getUser().getId().equals(u.getId()));
        if (!mine) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your ride");
        if (r.getStatus() != RideStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Receipt available after completion");
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=receipt-" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(receipts.pdf(r));
    }
}
