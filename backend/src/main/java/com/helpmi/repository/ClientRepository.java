package com.helpmi.repository;

import com.helpmi.domain.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ClientRepository extends JpaRepository<Client, UUID> {
    List<Client> findAllByOrderByNameAsc();
    List<Client> findByActiveOrderByNameAsc(boolean active);

    @Query("SELECT COUNT(t) FROM Ticket t JOIN t.clients c WHERE c.id = :clientId")
    long countTicketsByClientId(@Param("clientId") UUID clientId);
}
