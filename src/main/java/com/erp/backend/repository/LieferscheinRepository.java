package com.erp.backend.repository;

import com.erp.backend.domain.Lieferschein;
import com.erp.backend.domain.LieferscheinStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LieferscheinRepository extends JpaRepository<Lieferschein, UUID> {

    Optional<Lieferschein> findByLieferscheinnummer(String lieferscheinnummer);

    List<Lieferschein> findByAuftragId(Long auftragId);

    List<Lieferschein> findByCustomerId(UUID customerId);

    List<Lieferschein> findByStatus(LieferscheinStatus status);

    boolean existsByLieferscheinnummer(String lieferscheinnummer);
}