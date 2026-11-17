package com.cabgo.repo;

import com.cabgo.model.SupportTicket;
import com.cabgo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {
    List<SupportTicket> findByUserOrderByIdDesc(User user);
}
