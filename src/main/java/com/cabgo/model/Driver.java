package com.cabgo.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "drivers")
@Getter @Setter
public class Driver {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", unique = true)
    private User user;
    private String vehicleModel;
    private String vehiclePlate;
    private boolean available = false;
    private Double lat;
    private Double lng;
    private double ratingAvg = 0;
    private int ratingCount = 0;
    private double earnings = 0;
}
