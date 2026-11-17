package com.cabgo.config;

import com.cabgo.model.*;
import com.cabgo.repo.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Creates demo accounts and promo codes on first start. */
@Component
public class DataSeeder implements CommandLineRunner {
    private final UserRepository users;
    private final DriverRepository drivers;
    private final PromoCodeRepository promos;
    private final PasswordEncoder encoder;

    public DataSeeder(UserRepository users, DriverRepository drivers, PromoCodeRepository promos, PasswordEncoder encoder) {
        this.users = users;
        this.drivers = drivers;
        this.promos = promos;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        if (users.count() > 0) return;
        make("Admin", "admin@cab.com", "admin123", "9000000001", Role.ADMIN);
        make("Demo Rider", "rider@cab.com", "rider123", "9000000002", Role.RIDER);
        User du = make("Demo Driver", "driver@cab.com", "driver123", "9000000003", Role.DRIVER);
        Driver d = new Driver();
        d.setUser(du);
        d.setVehicleModel("Maruti Dzire");
        d.setVehiclePlate("GJ-01-AB-1234");
        d.setLat(23.0300);
        d.setLng(72.5800);
        drivers.save(d);
        promo("WELCOME20", 20);
        promo("SAVE10", 10);
    }

    private User make(String name, String email, String pass, String phone, Role role) {
        User u = new User();
        u.setName(name);
        u.setEmail(email);
        u.setPassword(encoder.encode(pass));
        u.setPhone(phone);
        u.setRole(role);
        return users.save(u);
    }

    private void promo(String code, int pct) {
        PromoCode p = new PromoCode();
        p.setCode(code);
        p.setPercentOff(pct);
        promos.save(p);
    }
}
