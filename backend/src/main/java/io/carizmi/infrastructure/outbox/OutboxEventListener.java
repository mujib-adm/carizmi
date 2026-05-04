package io.carizmi.infrastructure.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.carizmi.framework.event.DomainEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Transactional event listener that persists domain events to the outbox table
 * atomically within the same transaction as the domain change.
 *
 * <h2>Activation</h2>
 * <p>This component is <b>dormant by default</b>. It is only created when
 * {@code carizmi.outbox.relay.enabled=true} is set in the application configuration.</p>
 *
 * <p>The in-process event flow (Spring event bus → {@code DashboardProjector}) works
 * independently of this listener. This component is only needed when external consumers
 * (e.g., Google Cloud Pub/Sub) require event delivery through the
 * {@link OutboxRelayScheduler}.</p>
 *
 * <h2>To Activate</h2>
 * <ol>
 *   <li>Set {@code carizmi.outbox.relay.enabled=true} in {@code application.yml}</li>
 * </ol>
 *
 * @see OutboxRelayScheduler
 * @see io.carizmi.framework.event.DomainEventPublisher
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "carizmi.outbox.relay.enabled", havingValue = "true")
public class OutboxEventListener {

    private static final Logger logger = LoggerFactory.getLogger(OutboxEventListener.class);

    private final OutboxEvent outboxEvent;
    private final ObjectMapper objectMapper;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onDomainEvent(DomainEvent<?> event) {
        try {
            OutboxEventVO outboxEventVO = new OutboxEventVO();
            outboxEventVO.setAggregateType(event.aggregateType());
            outboxEventVO.setAggregateId(event.aggregateId());
            outboxEventVO.setEventType(event.eventType());
            outboxEventVO.setPayload(objectMapper.writeValueAsString(event.payload()));
            outboxEventVO.setCreatedAt(event.occurredAt());

            outboxEvent.save(outboxEventVO);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize domain event payload for {} [id={}]: {}",
                    event.aggregateType(), event.aggregateId(), e.getMessage(), e);
        }
    }
}