package com.cabgo.service;

import com.cabgo.dto.Dtos.BookRequest;
import com.cabgo.dto.Dtos.RatingRequest;
import com.cabgo.model.*;
import com.cabgo.repo.*;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class RideService {
    public static final Set<RideStatus> ACTIVE =
            EnumSet.of(RideStatus.REQUESTED, RideStatus.ACCEPTED, RideStatus.ARRIVED, RideStatus.STARTED);

    public record Quote(double distanceKm, double fare, double discount, int etaMinutes, String promoCode) {}

    private final RideRepository rides;
    private final DriverRepository drivers;
    private final PromoCodeRepository promos;
    private final RatingRepository ratings;
    private final FareService fares;
    private final SimpMessagingTemplate ws;

    public RideService(RideRepository rides, DriverRepository drivers, PromoCodeRepository promos,
                       RatingRepository ratings, FareService fares, SimpMessagingTemplate ws) {
        this.rides = rides;
        this.drivers = drivers;
        this.promos = promos;
        this.ratings = ratings;
        this.fares = fares;
        this.ws = ws;
    }

    public Quote quote(BookRequest r) {
        double km = fares.roadDistanceKm(r.pickupLat(), r.pickupLng(), r.dropLat(), r.dropLng());
        if (km < 0.2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pickup and drop are too close");
        }
        double fare = fares.fare(km);
        double discount = 0;
        String code = null;
        if (r.promoCode() != null && !r.promoCode().isBlank()) {
            PromoCode p = promos.findByCodeIgnoreCaseAndActiveTrue(r.promoCode().trim())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid promo code"));
            discount = FareService.round(fare * p.getPercentOff() / 100.0);
            code = p.getCode();
        }
        return new Quote(km, FareService.round(fare - discount), discount, fares.etaMinutes(km), code);
    }

    public Ride book(User rider, BookRequest r) {
        if (active(rider).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You already have an active ride");
        }
        Quote q = quote(r);
        Ride ride = new Ride();
        ride.setRider(rider);
        ride.setPickupLat(r.pickupLat());
        ride.setPickupLng(r.pickupLng());
        ride.setPickupAddress(r.pickupAddress() == null ? "Pickup point" : r.pickupAddress());
        ride.setDropLat(r.dropLat());
        ride.setDropLng(r.dropLng());
        ride.setDropAddress(r.dropAddress() == null ? "Drop point" : r.dropAddress());
        ride.setDistanceKm(q.distanceKm());
        ride.setFare(q.fare());
        ride.setDiscount(q.discount());
        ride.setPromoCode(q.promoCode());
        Ride saved = rides.save(ride);
        ws.convertAndSend("/topic/drivers", Map.of("type", "NEW_RIDE", "rideId", saved.getId()));
        return saved;
    }

    /** Requests within 15 km of an online driver. */
    public List<Ride> open(User driverUser) {
        Driver d = driverOf(driverUser);
        if (!d.isAvailable()) return List.of();
        return rides.findByStatus(RideStatus.REQUESTED).stream()
                .filter(r -> d.getLat() == null || FareService.haversineKm(
                        d.getLat(), d.getLng(), r.getPickupLat(), r.getPickupLng()) <= 15)
                .toList();
    }

    public Ride accept(User driverUser, Long id) {
        Driver d = driverOf(driverUser);
        if (!d.isAvailable()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Go online first");
        if (rides.findFirstByDriverAndStatusIn(d, ACTIVE).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Finish your current ride first");
        }
        Ride r = get(id);
        if (r.getStatus() != RideStatus.REQUESTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ride is no longer available");
        }
        r.setDriver(d);
        r.setStatus(RideStatus.ACCEPTED);
        r = rides.save(r);
        push(r);
        return r;
    }

    public Ride updateStatus(User driverUser, Long id, RideStatus to) {
        Driver d = driverOf(driverUser);
        Ride r = get(id);
        if (r.getDriver() == null || !r.getDriver().getId().equals(d.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your ride");
        }
        RideStatus from = r.getStatus();
        boolean ok = (from == RideStatus.ACCEPTED && to == RideStatus.ARRIVED)
                || (from == RideStatus.ARRIVED && to == RideStatus.STARTED)
                || (from == RideStatus.STARTED && to == RideStatus.COMPLETED);
        if (!ok) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status change " + from + " -> " + to);
        if (to == RideStatus.COMPLETED) {
            d.setEarnings(FareService.round(d.getEarnings() + r.getFare()));
            drivers.save(d);
        }
        r.setStatus(to);
        r = rides.save(r);
        push(r);
        return r;
    }

    public Ride cancel(User rider, Long id) {
        Ride r = get(id);
        if (!r.getRider().getId().equals(rider.getId())) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your ride");
        if (r.getStatus() != RideStatus.REQUESTED && r.getStatus() != RideStatus.ACCEPTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ride can no longer be cancelled");
        }
        r.setStatus(RideStatus.CANCELLED);
        r = rides.save(r);
        push(r);
        return r;
    }

    public void rate(User rider, Long id, RatingRequest q) {
        Ride r = get(id);
        if (!r.getRider().getId().equals(rider.getId())) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your ride");
        if (r.getStatus() != RideStatus.COMPLETED || r.getDriver() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only completed rides can be rated");
        }
        if (r.isRated()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Already rated");
        Rating x = new Rating();
        x.setRide(r);
        x.setStars(q.stars());
        x.setComment(q.comment());
        ratings.save(x);
        Driver d = drivers.findById(r.getDriver().getId()).orElseThrow();
        d.setRatingAvg(FareService.round((d.getRatingAvg() * d.getRatingCount() + q.stars()) / (d.getRatingCount() + 1)));
        d.setRatingCount(d.getRatingCount() + 1);
        drivers.save(d);
        r.setRated(true);
        rides.save(r);
    }

    public Optional<Ride> active(User u) {
        if (u.getRole() == Role.DRIVER) {
            return drivers.findByUser(u).flatMap(d -> rides.findFirstByDriverAndStatusIn(d, ACTIVE));
        }
        if (u.getRole() == Role.RIDER) {
            return rides.findByRiderOrderByCreatedAtDesc(u).stream()
                    .filter(r -> ACTIVE.contains(r.getStatus()) || (r.getStatus() == RideStatus.COMPLETED && !r.isPaid()))
                    .findFirst();
        }
        return Optional.empty();
    }

    public List<Ride> history(User u) {
        if (u.getRole() == Role.DRIVER) return rides.findByDriverOrderByCreatedAtDesc(driverOf(u));
        if (u.getRole() == Role.ADMIN) return rides.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        return rides.findByRiderOrderByCreatedAtDesc(u);
    }

    public Ride get(Long id) {
        return rides.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ride not found"));
    }

    public Driver driverOf(User u) {
        return drivers.findByUser(u).orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a driver"));
    }

    public void push(Ride r) {
        ws.convertAndSend("/topic/ride/" + r.getId(), Map.of("type", "STATUS", "status", r.getStatus().name()));
        ws.convertAndSend("/topic/drivers", Map.of("type", "REFRESH"));
    }
}
