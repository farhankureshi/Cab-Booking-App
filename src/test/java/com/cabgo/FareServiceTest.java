package com.cabgo;

import com.cabgo.service.FareService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FareServiceTest {
    private final FareService fares = new FareService(50, 12, 80);

    @Test
    void minimumFareApplies() {
        assertEquals(80.0, fares.fare(1.0));
    }

    @Test
    void perKmFareApplies() {
        assertEquals(170.0, fares.fare(10.0));
    }

    @Test
    void haversineZeroForSamePoint() {
        assertEquals(0.0, FareService.haversineKm(23.0225, 72.5714, 23.0225, 72.5714), 1e-9);
    }

    @Test
    void ahmedabadToGandhinagarIsAboutTwentyKm() {
        double km = FareService.haversineKm(23.0225, 72.5714, 23.2156, 72.6369);
        assertTrue(km > 18 && km < 25);
    }
}
