package com.cabgo.web;

import com.cabgo.dto.Dtos.LocationRequest;
import com.cabgo.model.Driver;
import com.cabgo.model.User;
import com.cabgo.repo.DriverRepository;
import com.cabgo.repo.RatingRepository;
import com.cabgo.repo.RideRepository;
import com.cabgo.model.RideStatus;
import com.cabgo.service.RideService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/driver")
@PreAuthorize("hasRole('DRIVER')")
public class DriverController {
    private final RideService rideService;
    private final DriverRepository drivers;
    private final RideRepository rides;
    private final RatingRepository ratings;

    public DriverController(RideService rideService, DriverRepository drivers, RideRepository rides, RatingRepository ratings) {
        this.rideService = rideService;
        this.drivers = drivers;
        this.rides = rides;
        this.ratings = ratings;
    }

    @PutMapping("/status")
    public Driver status(@AuthenticationPrincipal User u, @RequestBody Map<String, Boolean> body) {
        Driver d = rideService.driverOf(u);
        d.setAvailable(Boolean.TRUE.equals(body.get("available")));
        return drivers.save(d);
    }

    @PutMapping("/location")
    public Driver location(@AuthenticationPrincipal User u, @RequestBody LocationRequest r) {
        Driver d = rideService.driverOf(u);
        d.setLat(r.lat());
        d.setLng(r.lng());
        return drivers.save(d);
    }

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard(@AuthenticationPrincipal User u) {
        Driver d = rideService.driverOf(u);
        long completed = rides.findByDriverOrderByCreatedAtDesc(d).stream()
                .filter(r -> r.getStatus() == RideStatus.COMPLETED).count();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("driver", d);
        m.put("completed", completed);
        m.put("reviews", ratings.findTop10ByRideDriverOrderByIdDesc(d).stream()
                .map(r -> Map.of("stars", r.getStars(), "comment", r.getComment() == null ? "" : r.getComment()))
                .toList());
        return m;
    }
}
