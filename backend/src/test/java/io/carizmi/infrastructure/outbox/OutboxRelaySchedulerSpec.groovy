package io.carizmi.infrastructure.outbox

import io.carizmi.testbase.BaseSpecification

import java.time.Instant

class OutboxRelaySchedulerSpec extends BaseSpecification {

    OutboxEvent outboxEvent = Mock()

    OutboxRelayScheduler scheduler = new OutboxRelayScheduler(outboxEvent)

    def "test relayEvents - Should process pending events and mark as processed"() {
        given:
        OutboxEventVO event1 = new OutboxEventVO(id: 1L, aggregateType: "PaymentVO", aggregateId: 10, eventType: "CREATED", payload: '{}', createdAt: Instant.now())
        OutboxEventVO event2 = new OutboxEventVO(id: 2L, aggregateType: "MemberVO", aggregateId: 20, eventType: "UPDATED", payload: '{}', createdAt: Instant.now())

        when: "The target method executed"
        scheduler.relayEvents()

        then: "The expected calls are made"
        1 * outboxEvent.findPendingEvents() >> [event1, event2]
        0 * _

        and: "Both events are marked as processed"
        event1.processedAt != null
        event2.processedAt != null
        noExceptionThrown()
    }
}