package com.cabgo.web;

import com.cabgo.dto.Dtos.LoginRequest;
import com.cabgo.dto.Dtos.RegisterRequest;
import com.cabgo.model.Driver;
import com.cabgo.model.Role;
import com.cabgo.model.User;
import com.cabgo.repo.DriverRepository;
import com.cabgo.repo.UserRepository;
import com.cabgo.service.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserRepository users;
    private final DriverRepository drivers;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthController(UserRepository users, DriverRepository drivers, PasswordEncoder encoder, JwtService jwt) {
        this.users = users;
        this.drivers = drivers;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    @PostMapping("/register")
    public Map<String, Object> register(@Valid @RequestBody RegisterRequest r) {
        String email = r.email().trim().toLowerCase();
        if (users.findByEmail(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }
        Role role = r.role() == Role.DRIVER ? Role.DRIVER : Role.RIDER;   // ADMIN cannot self-register
        User u = new User();
        u.setName(r.name().trim());
        u.setEmail(email);
        u.setPassword(encoder.encode(r.password()));   // BCrypt
        u.setPhone(r.phone());
        u.setRole(role);
        users.save(u);
        if (role == Role.DRIVER) {
            Driver d = new Driver();
            d.setUser(u);
            d.setVehicleModel(r.vehicleModel());
            d.setVehiclePlate(r.vehiclePlate());
            drivers.save(d);
        }
        return response(u);
    }

    @PostMapping("/login")
    public Map<String, Object> login(@Valid @RequestBody LoginRequest r) {
        User u = users.findByEmail(r.email().trim().toLowerCase())
                .filter(x -> encoder.matches(r.password(), x.getPassword()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        return response(u);
    }

    private Map<String, Object> response(User u) {
        return Map.of("token", jwt.generate(u),
                "user", Map.of("id", u.getId(), "name", u.getName(), "email", u.getEmail(), "role", u.getRole().name()));
    }
}
