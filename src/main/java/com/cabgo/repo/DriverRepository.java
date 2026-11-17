package com.cabgo.repo;

import com.cabgo.model.Driver;
import com.cabgo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface DriverRepository extends JpaRepository<Driver, Long> {
    Optional<Driver> findByUser(User user);
    List<Driver> findTop10ByRatingCountGreaterThanOrderByRatingAvgDesc(int count);
}
