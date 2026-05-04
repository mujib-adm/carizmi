package io.carizmi.infrastructure.outbox

import io.carizmi.testbase.BaseSpecification

import java.time.Instant

class OutboxEventSpec extends BaseSpecification {

    OutboxEventRepository outboxEventRepo = Mock()

    OutboxEventImpl outboxEventService = new OutboxEventImpl(outboxEventRepo)

    def "test save: Should persist outbox event to repository"() {
        given:
        OutboxEventVO outboxEventVO = new OutboxEventVO(
                aggregateType: "PaymentVO",
                aggregateId: 42,
                eventType: "CREATED",
                payload: '{"amount": 60.00}',
                createdAt: Instant.now()
        )

        when: "The target method executed"
        outboxEventService.save(outboxEventVO)

        then: "The expected calls are made"
        1 * outboxEventRepo.save(outboxEventVO)
        0 * _

        and: "The expected result"
        noExceptionThrown()
    }

    def "test findPendingEvents - Should return unprocessed events ordered by creation time"() {
        given:
        OutboxEventVO event1 = new OutboxEventVO(id: 1L, aggregateType: "PaymentVO", aggregateId: 10, eventType: "CREATED", payload: '{}', createdAt: Instant.now())
        OutboxEventVO event2 = new OutboxEventVO(id: 2L, aggregateType: "MemberVO", aggregateId: 20, eventType: "UPDATED", payload: '{}', createdAt: Instant.now())

        when: "The target method executed"
        List<OutboxEventVO> result = outboxEventService.findPendingEvents()

        then: "The expected calls are made"
        1 * outboxEventRepo.findTop100ByProcessedAtIsNullOrderByCreatedAtAsc() >> [event1, event2]
        0 * _

        and: "The expected result"
        result.size() == 2
        result[0].aggregateType == "PaymentVO"
        result[1].aggregateType == "MemberVO"
        noExceptionThrown()
    }

    def "test findPendingEvents - Should return empty list when no pending events"() {
        when: "The target method executed"
        List<OutboxEventVO> result = outboxEventService.findPendingEvents()

        then: "The expected calls are made"
        1 * outboxEventRepo.findTop100ByProcessedAtIsNullOrderByCreatedAtAsc() >> []
        0 * _

        and: "The expected result"
        result.isEmpty()
        noExceptionThrown()
    }
}