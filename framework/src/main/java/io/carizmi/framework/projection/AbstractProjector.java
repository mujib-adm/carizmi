package io.carizmi.framework.projection;

import io.carizmi.framework.event.DomainEvent;
import org.slf4j.Logger;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Set;

/**
 * Base abstract class for CQRS Read-Side Projectors.
 *
 * <p>This class implements the template method pattern for event-driven read model
 * projections, ensuring that event filtering, error handling, and lifecycle hooks
 * are executed consistently across all projectors.</p>
 *
 * <h2>Architecture Overview</h2>
 * <p>
 * This is the <b>read-side counterpart</b> to {@link io.carizmi.framework.bl.AbstractBusinessLogic},
 * which governs write-side CRUD operations. Together they form the two halves of the
 * CQRS (Command Query Responsibility Segregation) architecture:
 * </p>
 * <ul>
 *   <li><b>Write-Side</b> ({@code AbstractBusinessLogic}): Controller → BL → Validator → Repository → DomainEvent</li>
 *   <li><b>Read-Side</b> ({@code AbstractProjector}): DomainEvent → Projector → ReadModel Repository → Service → Controller</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <p>Subclasses must implement three abstract methods:</p>
 * <ul>
 *   <li>{@link #getLogger()} — provides the logger for the concrete projector</li>
 *   <li>{@link #supportedAggregateTypes()} — declares which domain events trigger this projector</li>
 *   <li>{@link #rebuildProjection()} — the actual computation and persistence of the read model</li>
 * </ul>
 *
 * <p>Subclasses may optionally override lifecycle hooks:</p>
 * <ul>
 *   <li>{@link #beforeRebuild(DomainEvent)} — called before {@code rebuildProjection()}</li>
 *   <li>{@link #afterRebuild(DomainEvent)} — called after successful {@code rebuildProjection()}</li>
 * </ul>
 */
public abstract class AbstractProjector {

    // ─── Abstract Contract (subclass MUST implement) ───────────────────

    /**
     * Returns the logger for the concrete projector subclass.
     *
     * @return the SLF4J logger
     */
    protected abstract Logger getLogger();

    /**
     * Declares which aggregate types trigger this projector.
     *
     * <p>Return a {@link Set} of ValueObject simple class names
     * (e.g., {@code Set.of("PaymentVO", "MemberVO", "ExpenseVO")}).
     * Only domain events matching these types will invoke {@link #rebuildProjection()}.</p>
     *
     * @return immutable set of supported aggregate type names
     */
    protected abstract Set<String> supportedAggregateTypes();

    /**
     * Rebuilds the read model from current data.
     *
     * <p>This method should query the necessary domain services, compute the
     * denormalized read model, and persist it to the snapshot table.
     * Implementations should annotate this method with {@code @Transactional}.</p>
     */
    protected abstract void rebuildProjection();

    // ─── Lifecycle Hooks (subclass MAY override) ───────────────────────

    /**
     * Hook called before {@link #rebuildProjection()}.
     * Override to add pre-rebuild logic (e.g., logging, metrics).
     *
     * @param event the domain event that triggered the projection
     */
    protected void beforeRebuild(DomainEvent<?> event) {
        // No-op by default
    }

    /**
     * Hook called after a successful {@link #rebuildProjection()}.
     * Override to add post-rebuild logic (e.g., cache invalidation, notifications).
     *
     * @param event the domain event that triggered the projection
     */
    protected void afterRebuild(DomainEvent<?> event) {
        // No-op by default
    }

    // ─── Template Method (framework controls the flow) ─────────────────

    /**
     * Receives domain events after the originating transaction commits and
     * dispatches to {@link #rebuildProjection()} if the event's aggregate type
     * is supported by this projector.
     *
     * <p>This method is {@code final} — the framework owns the lifecycle.
     * Subclasses customize behavior via abstract methods and lifecycle hooks.</p>
     *
     * <p>Execution is {@code @Async} to avoid blocking the write-side thread.</p>
     *
     * @param event the domain event published by {@link io.carizmi.framework.bl.AbstractBusinessLogic}
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public final void onDomainEvent(DomainEvent<?> event) {
        if (!supportedAggregateTypes().contains(event.aggregateType())) {
            return;
        }
        getLogger().info("Projection triggered by {} {} [id={}]", event.eventType(), event.aggregateType(), event.aggregateId());
        try {
            beforeRebuild(event);
            rebuildProjection();
            afterRebuild(event);
        } catch (Exception e) {
            getLogger().error("Projection rebuild failed for {} {} [id={}]: {}",
                    event.eventType(), event.aggregateType(), event.aggregateId(), e.getMessage(), e);
        }
    }
}