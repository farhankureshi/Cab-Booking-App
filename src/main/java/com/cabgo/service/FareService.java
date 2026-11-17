package com.cabgo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class FareService {
    private final double base;
    private final double perKm;
    private final double minFare;

    public FareService(@Value("${app.fare.base:50}") double base,
                       @Value("${app.fare.per-km:12}") double perKm,
                       @Value("${app.fare.minimum:80}") double minFare) {
        this.base = base;
        this.perKm = perKm;
        this.minFare = minFare;
    }

    public static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double r = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return r * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    /** Straight-line distance x 1.3 as a road-distance approximation (distance-matrix substitute). */
    public double roadDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        return round(haversineKm(lat1, lon1, lat2, lon2) * 1.3);
    }

    public double fare(double km) {
        return round(Math.max(minFare, base + perKm * km));
    }

    public int etaMinutes(double km) {
        return (int) Math.ceil(km / 30.0 * 60.0);
    }

    public static double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
