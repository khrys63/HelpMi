package com.helpmi.repository;

import com.helpmi.domain.Label;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LabelRepository extends JpaRepository<Label, UUID> {
    List<Label> findAllByOrderByNameAsc();
    List<Label> findByNameContainingIgnoreCaseOrderByNameAsc(String name);
    Optional<Label> findByNameIgnoreCase(String name);

    @Query("SELECT COUNT(t) FROM Ticket t JOIN t.labels l WHERE l.id = :labelId")
    long countTicketsByLabelId(@Param("labelId") UUID labelId);
}
