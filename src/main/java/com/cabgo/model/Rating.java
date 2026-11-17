package com.cabgo.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Entity
@Table(name = "ratings")
@Getter @Setter
public class Rating {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne(optional = false)
    @JoinColumn(name = "ride_id", unique = true)
    private Ride ride;
    private int stars;
    @Column(name = "review_text", length = 1000)
    private String comment;
    private Instant createdAt = Instant.now();
}
