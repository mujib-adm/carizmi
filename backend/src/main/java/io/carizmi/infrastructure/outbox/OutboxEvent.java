package io.carizmi.infrastructure.outbox;

import java.util.List;

public interface OutboxEvent {

    void save(OutboxEventVO outboxEventVO);

    List<OutboxEventVO> findPendingEvents();
}