package io.carizmi.framework.event;

import io.carizmi.framework.vo.ValueObject;

import java.time.Instant;

/**
 * Immutable domain event emitted by {@link io.carizmi.framework.bl.AbstractBusinessLogic}
 * after a successful add, update, or delete operation.
 *
 * <p>This event is published via Spring's {@link org.springframework.context.ApplicationEventPublisher}
 * and consumed in-process by registered listeners (e.g., {@code AbstractProjector} subclasses).
 * When the Transactional Outbox relay is enabled ({@code carizmi.outbox.relay.enabled=true}),
 * events are additionally persisted to the {@code outbox_event} table for external delivery.</p>
 *
 * @param eventType     the operation type: "CREATED", "UPDATED", or "DELETED"
 * @param aggregateType the simple class name of the ValueObject (e.g., "PaymentVO")
 * @param aggregateId   the primary key of the affected entity
 * @param payload       the ValueObject instance at the time of the event
 * @param occurredAt    the timestamp when the event was generated
 */
public record DomainEvent<V extends ValueObject>(
        String eventType,
        String aggregateType,
        Integer aggregateId,
        V payload,
        Instant occurredAt) {
}