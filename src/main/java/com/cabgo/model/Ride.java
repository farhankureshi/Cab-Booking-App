package com.cabgo.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Entity
@Table(name = "rides")
@Getter @Setter
public class Ride {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Version
    private Long version;
    @ManyToOne(optional = false)
    private User rider;
    @ManyToOne
    private Driver driver;
    private String pickupAddress;
    private double pickupLat;
    private double pickupLng;
    private String dropAddress;
    private double dropLat;
    private double dropLng;
    private double distanceKm;
    private double fare;
    private double discount;
    private String promoCode;
    @Enumerated(EnumType.STRING)
    private RideStatus status = RideStatus.REQUESTED;
    private boolean paid = false;
    private boolean rated = false;
    private Instant createdAt = Instant.now();
}
