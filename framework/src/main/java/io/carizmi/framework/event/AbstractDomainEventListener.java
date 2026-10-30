package io.carizmi.framework.event;

import org.slf4j.Logger;
import org.springframework.context.event.EventListener;

import java.util.Set;

/**
 * Base abstract class for synchronous in-transaction domain event listeners.
 *
 * <p>This class implements the template method pattern for domain event listeners,
 * ensuring that event filtering, logging, and error handling are executed consistently
 * across all domain listeners.</p>
 *
 * <h2>Architecture Overview</h2>
 * <p>
 * This is the <b>synchronous write-side counterpart</b> to
 * {@link io.carizmi.framework.projection.AbstractProjector}. Together with
 * {@link io.carizmi.framework.bl.AbstractBusinessLogic}, they define the platform's
 * unified event processing model:
 * </p>
 * <ul>
 *   <li><b>Write-Side CRUD & Publishing:</b> {@link io.carizmi.framework.bl.AbstractBusinessLogic}
 *       saves the entity and automatically publishes a {@link DomainEvent} via
 *       {@link DomainEventPublisher} when {@code publishesDomainEvents()} is true.</li>
 *   <li><b>Synchronous In-Transaction Side Effects:</b> {@link AbstractDomainEventListener}
 *       receives the {@link DomainEvent} synchronously within the same active transaction,
 *       enforcing atomic multi-domain operations (e.g., member activation upon registration fee receipt).</li>
 *   <li><b>Asynchronous Read-Side Projections:</b> {@link io.carizmi.framework.projection.AbstractProjector}
 *       receives the {@link DomainEvent} after commit asynchronously to rebuild read models.</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <p>Subclasses must implement three methods:</p>
 * <ul>
 *   <li>{@link #getLogger()} — provides the logger for the concrete listener</li>
 *   <li>{@link #supportedAggregateTypes()} — declares which domain events trigger this listener</li>
 *   <li>{@link #onEvent(DomainEvent)} — the synchronous domain logic to execute</li>
 * </ul>
 *
 * <p>Subclasses may optionally override lifecycle hooks:</p>
 * <ul>
 *   <li>{@link #beforeHandle(DomainEvent)} — called before {@code onEvent()}</li>
 *   <li>{@link #afterHandle(DomainEvent)} — called after successful {@code onEvent()}</li>
 * </ul>
 */
public abstract class AbstractDomainEventListener {

    // ─── Abstract Contract (subclass MUST implement) ───────────────────

    /**
     * Returns the logger for the concrete listener subclass.
     *
     * @return the SLF4J logger
     */
    protected abstract Logger getLogger();

    /**
     * Declares which aggregate types trigger this listener.
     *
     * <p>Return a {@link Set} of ValueObject simple class names
     * (e.g., {@code Set.of("PaymentVO")}). Only domain events matching these types
     * will invoke {@link #onEvent(DomainEvent)}.</p>
     *
     * @return immutable set of supported aggregate type names
     */
    protected abstract Set<String> supportedAggregateTypes();

    /**
     * Processes the domain event synchronously within the originating transaction.
     *
     * @param event the domain event published by {@link io.carizmi.framework.bl.AbstractBusinessLogic}
     */
    protected abstract void onEvent(DomainEvent<?> event);

    // ─── Lifecycle Hooks (subclass MAY override) ───────────────────────

    /**
     * Hook called before {@link #onEvent(DomainEvent)}.
     * Override to add pre-processing logic (e.g., logging, metrics).
     *
     * @param event the domain event being handled
     */
    protected void beforeHandle(DomainEvent<?> event) {
        // No-op by default
    }

    /**
     * Hook called after successful {@link #onEvent(DomainEvent)}.
     * Override to add post-processing logic (e.g., auditing, metrics).
     *
     * @param event the domain event that was handled
     */
    protected void afterHandle(DomainEvent<?> event) {
        // No-op by default
    }

    // ─── Template Method (framework controls the flow) ─────────────────

    /**
     * Receives domain events synchronously within the active transaction and
     * dispatches to {@link #onEvent(DomainEvent)} if the event's aggregate type
     * is supported by this listener.
     *
     * <p>This method is {@code final} — the framework owns the filtering and lifecycle.
     * Subclasses customize behavior via abstract methods and lifecycle hooks.</p>
     *
     * @param event the domain event published by {@link io.carizmi.framework.bl.AbstractBusinessLogic}
     */
    @EventListener
    public final void onDomainEvent(DomainEvent<?> event) {
        if (!supportedAggregateTypes().contains(event.aggregateType())) {
            return;
        }
        getLogger().info("Domain event received by {}: {} {} [id={}]",
                getClass().getSimpleName(), event.eventType(), event.aggregateType(), event.aggregateId());
        try {
            beforeHandle(event);
            onEvent(event);
            afterHandle(event);
        } catch (Exception e) {
            getLogger().error("Domain event processing failed in {} for {} {} [id={}]: {}",
                    getClass().getSimpleName(), event.eventType(), event.aggregateType(), event.aggregateId(), e.getMessage(), e);
            throw e;
        }
    }
}