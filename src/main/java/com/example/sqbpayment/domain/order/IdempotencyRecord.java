package com.example.sqbpayment.domain.order;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "idempotency_record")
public class IdempotencyRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 256)
    private String idempotencyKey;

    @Column(name = "event_type", length = 64)
    private String eventType;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    protected IdempotencyRecord() {}

    public IdempotencyRecord(String idempotencyKey, String eventType) {
        this.idempotencyKey = idempotencyKey;
        this.eventType = eventType;
        this.processedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getEventType() { return eventType; }
    public LocalDateTime getProcessedAt() { return processedAt; }
}
