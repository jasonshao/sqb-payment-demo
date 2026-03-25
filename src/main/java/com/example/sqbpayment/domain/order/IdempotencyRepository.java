package com.example.sqbpayment.domain.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IdempotencyRepository extends JpaRepository<IdempotencyRecord, Long> {
    boolean existsByIdempotencyKey(String idempotencyKey);
}
