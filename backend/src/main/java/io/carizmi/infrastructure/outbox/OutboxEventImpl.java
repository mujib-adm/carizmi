package io.carizmi.infrastructure.outbox;

import io.carizmi.framework.annotation.RepositoryOwnerFor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@RepositoryOwnerFor(OutboxEventRepository.class)
public class OutboxEventImpl implements OutboxEvent {

    private final OutboxEventRepository outboxEventRepo;

    /**
     * Persists a domain event to the outbox table within the caller's existing transaction.
     *
     * <p>Uses {@code Propagation.MANDATORY} to enforce that this method is never called
     * without an active transaction. This is the core guarantee of the Transactional Outbox
     * Pattern: the outbox record and the domain change are committed or rolled back as a
     * single atomic unit, ensuring the event is never written without a domain change transaction.</p>
     *
     * <p>Calling this method without an active transaction will throw
     * {@code IllegalTransactionStateException}, a clear signal that the outbox contract is violated.</p>
     */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void save(OutboxEventVO outboxEventVO) {
        outboxEventRepo.save(outboxEventVO);
    }

    @Override
    public List<OutboxEventVO> findPendingEvents() {
        return outboxEventRepo.findTop100ByProcessedAtIsNullOrderByCreatedAtAsc();
    }
}