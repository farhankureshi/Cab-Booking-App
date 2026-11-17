package com.cabgo.repo;

import com.cabgo.model.Driver;
import com.cabgo.model.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RatingRepository extends JpaRepository<Rating, Long> {
    List<Rating> findTop10ByRideDriverOrderByIdDesc(Driver driver);
}
