package io.carizmi.infrastructure.outbox;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEventVO, Long> {

    /**
     * @return list of pending events (oldest 100) ordered by creation time (FIFO)
     */
    List<OutboxEventVO> findTop100ByProcessedAtIsNullOrderByCreatedAtAsc();
}