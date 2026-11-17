package com.cabgo.repo;

import com.cabgo.model.PromoCode;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PromoCodeRepository extends JpaRepository<PromoCode, Long> {
    Optional<PromoCode> findByCodeIgnoreCaseAndActiveTrue(String code);
}
