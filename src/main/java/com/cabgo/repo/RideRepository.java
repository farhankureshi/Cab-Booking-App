package com.cabgo.repo;

import com.cabgo.model.Driver;
import com.cabgo.model.Ride;
import com.cabgo.model.RideStatus;
import com.cabgo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RideRepository extends JpaRepository<Ride, Long> {
    List<Ride> findByRiderOrderByCreatedAtDesc(User rider);
    List<Ride> findByDriverOrderByCreatedAtDesc(Driver driver);
    List<Ride> findByStatus(RideStatus status);
    Optional<Ride> findFirstByDriverAndStatusIn(Driver driver, Collection<RideStatus> statuses);
    long countByStatus(RideStatus status);
}
