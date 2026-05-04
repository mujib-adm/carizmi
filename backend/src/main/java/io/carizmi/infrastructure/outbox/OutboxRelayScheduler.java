package io.carizmi.infrastructure.outbox;

// import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
// import java.util.Map;

/**
 * Polls the outbox event table for unprocessed domain events and relays them
 * to external broker.
 *
 * <p>This is the "delivery truck" of the Transactional Outbox Pattern. Events
 * are already atomically persisted by {@link OutboxEventListener}. This scheduler
 * picks them up in FIFO order and publishes to the message broker.</p>
 *
 * <h2>Activation</h2>
 * <p>This component is <b>dormant by default</b>. It is only created when
 * {@code carizmi.outbox.relay.enabled=true} is set in the application configuration.</p>
 *
 * <p>Both this scheduler and {@link OutboxEventListener} are gated behind the same
 * configuration flag because they form a pair — the listener writes to the outbox,
 * and this scheduler reads from it. Enabling one without the other is meaningless.</p>
 *
 * <h2>To Activate (using Google Cloud Pub/Sub as an example) </h2>
 * <ol>
 *   <li>Set {@code carizmi.outbox.relay.enabled=true} in {@code application.yml}</li>
 *   <li>Enable Pub/Sub: {@code spring.cloud.gcp.pubsub.enabled=true}</li>
 *   <li>Configure GCP project: {@code spring.cloud.gcp.pubsub.project-id=${GCP_PROJECT_ID}}</li>
 *   <li>Create the GCP topic: {@code gcloud pubsub topics create domain-events}</li>
 *   <li>Create a subscription: {@code gcloud pubsub subscriptions create domain-events-subscription --topic=domain-events}</li>
 * </ol>
 *
 * <h2>Scaling Path</h2>
 * <ul>
 *   <li><b>Low (<1K events/day):</b> 10-second polling with batch of 100. No scaling concerns.</li>
 *   <li><b>Medium (~1K events/day):</b> Reduce {@code fixedDelay} to 1–2 seconds. Consider
 *       partitioning by {@code aggregateType} for parallel processing.</li>
 *   <li><b>High (~100K+ events/day):</b> Replace polling with Change Data Capture (CDC) via
 *       Debezium, which streams outbox changes directly from the database binlog, eliminating
 *       polling latency entirely and removing the need for this scheduler.</li>
 * </ul>
 *
 * @see OutboxEventListener
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "carizmi.outbox.relay.enabled", havingValue = "true")
public class OutboxRelayScheduler {

    private static final Logger logger = LoggerFactory.getLogger(OutboxRelayScheduler.class);
    private static final String TOPIC = "domain-events";

    private final OutboxEvent outboxEvent;
    // private final PubSubTemplate pubSubTemplate;

    /**
     * Polls for unprocessed outbox events and relays them to the external broker.
     */
    @Scheduled(fixedDelay = 10_000)
    @Transactional
    @CircuitBreaker(name = "outboxRelay", fallbackMethod = "relayFallback")
    public void relayEvents() {
        List<OutboxEventVO> pendingOutboxEventVOs = outboxEvent.findPendingEvents();
        if (pendingOutboxEventVOs.isEmpty()) {
            return;
        }
        logger.info("Outbox relay: processing {} pending event(s)", pendingOutboxEventVOs.size());
        for (OutboxEventVO outboxEventVO : pendingOutboxEventVOs) {
            try {
                publishToBroker(outboxEventVO);
                outboxEventVO.setProcessedAt(Instant.now());
            } catch (Exception e) {
                logger.error("Failed to relay outbox event [id={}, type={}/{}]: {}",
                        outboxEventVO.getId(), outboxEventVO.getEventType(), outboxEventVO.getAggregateType(),
                        e.getMessage(), e);
                break; // Maintain FIFO ordering — retry this event first
            }
        }
    }

    /**
     * Publishes a single outbox event to the external broker (like Google Cloud Pub/Sub).
     *
     * <p>Events are published to a single topic ({@value #TOPIC}) with metadata
     * headers for routing. Consumers can filter by {@code aggregateType} and
     * {@code eventType} to subscribe only to relevant events.</p>
     */
    private void publishToBroker(OutboxEventVO outboxEventVO) {
        // pubSubTemplate.publish(
        //         TOPIC,
        //         outboxEventVO.getPayload(),
        //         Map.of(
        //                 "aggregateType", outboxEventVO.getAggregateType(),
        //                 "aggregateId", String.valueOf(outboxEventVO.getAggregateId()),
        //                 "eventType", outboxEventVO.getEventType()
        //         )
        // );

        // logger.debug("Published to Pub/Sub: [{}] {} id={}",
        //         outboxEventVO.getEventType(), outboxEventVO.getAggregateType(), outboxEventVO.getAggregateId());
    }

    /**
     * Resilience4j fallback when the circuit breaker is open.
     * Events remain safely in the outbox table until the broker recovers.
     */
    @SuppressWarnings("unused")
    private void relayFallback(Exception e) {
        logger.warn("Outbox relay circuit is OPEN — broker appears unavailable. "
                + "Events will be delivered when the broker recovers. "
                + "Cause: {}", e.getMessage());
    }
}