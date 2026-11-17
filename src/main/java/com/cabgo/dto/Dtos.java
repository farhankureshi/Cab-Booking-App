package com.cabgo.dto;

import com.cabgo.model.Role;
import jakarta.validation.constraints.*;

public final class Dtos {
    private Dtos() {}

    public record RegisterRequest(@NotBlank String name, @NotBlank @Email String email,
                                  @NotBlank @Size(min = 6) String password, String phone, Role role,
                                  String vehicleModel, String vehiclePlate) {}

    public record LoginRequest(@NotBlank String email, @NotBlank String password) {}

    public record BookRequest(@NotNull Double pickupLat, @NotNull Double pickupLng, String pickupAddress,
                              @NotNull Double dropLat, @NotNull Double dropLng, String dropAddress,
                              String promoCode) {}

    public record RatingRequest(@Min(1) @Max(5) int stars, String comment) {}

    public record SupportRequest(@NotBlank String subject, @NotBlank String message) {}

    public record LocationRequest(double lat, double lng) {}
}
