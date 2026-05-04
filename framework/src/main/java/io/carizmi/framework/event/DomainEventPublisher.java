package io.carizmi.framework.event;

import io.carizmi.framework.vo.ValueObject;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Publisher for domain events.
 *
 * <p>Delegates to Spring's {@link ApplicationEventPublisher} to emit {@link DomainEvent} instances.
 * These events are consumed in-process by any registered Spring event listener, such as
 * {@link io.carizmi.framework.projection.AbstractProjector} subclasses.</p>
 *
 * <p>When the Transactional Outbox relay is enabled ({@code carizmi.outbox.relay.enabled=true}),
 * events are additionally persisted to the {@code outbox_event} table for external delivery.</p>
 *
 * <p><b>Note:</b> This component is injected into {@link io.carizmi.framework.bl.AbstractBusinessLogic}
 * so that event publication is automatic for all domain entities that opt in
 * via {@code publishesDomainEvents()} — no per-domain wiring required.</p>
 */
@Component
public class DomainEventPublisher {

    private final ApplicationEventPublisher springPublisher;

    public DomainEventPublisher(ApplicationEventPublisher springPublisher) {
        this.springPublisher = springPublisher;
    }

    /**
     * Publishes a domain event for the given ValueObject.
     *
     * @param eventType the operation type ("CREATED", "UPDATED", "DELETED")
     * @param vo        the ValueObject instance
     * @param id        the primary key of the entity
     */
    public <V extends ValueObject> void publish(String eventType, V vo, Integer id) {
        springPublisher.publishEvent(new DomainEvent<>(
                eventType,
                vo.getClass().getSimpleName(),
                id,
                vo,
                Instant.now()
        ));
    }
}