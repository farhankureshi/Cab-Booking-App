package com.cabgo.web;

import com.cabgo.model.Payment;
import com.cabgo.model.RideStatus;
import com.cabgo.repo.DriverRepository;
import com.cabgo.repo.PaymentRepository;
import com.cabgo.repo.RideRepository;
import com.cabgo.repo.UserRepository;
import com.cabgo.service.FareService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private final UserRepository users;
    private final DriverRepository drivers;
    private final RideRepository rides;
    private final PaymentRepository payments;

    public AdminController(UserRepository users, DriverRepository drivers, RideRepository rides, PaymentRepository payments) {
        this.users = users;
        this.drivers = drivers;
        this.rides = rides;
        this.payments = payments;
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("users", users.count());
        m.put("drivers", drivers.count());
        m.put("rides", rides.count());
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (RideStatus s : RideStatus.values()) byStatus.put(s.name(), rides.countByStatus(s));
        m.put("ridesByStatus", byStatus);
        double revenue = payments.findAll().stream().filter(p -> "PAID".equals(p.getStatus()))
                .mapToDouble(Payment::getAmount).sum();
        m.put("revenue", FareService.round(revenue));
        m.put("topDrivers", drivers.findTop10ByRatingCountGreaterThanOrderByRatingAvgDesc(0).stream()
                .map(d -> Map.of("name", d.getUser().getName(),
                        "vehicle", Objects.toString(d.getVehicleModel(), ""),
                        "ratingAvg", d.getRatingAvg(), "ratingCount", d.getRatingCount()))
                .toList());
        return m;
    }
}
