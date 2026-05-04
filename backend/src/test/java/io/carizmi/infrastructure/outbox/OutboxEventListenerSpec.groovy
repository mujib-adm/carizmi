package io.carizmi.infrastructure.outbox

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import io.carizmi.domain.finance.model.PaymentVO
import io.carizmi.framework.event.DomainEvent
import io.carizmi.testbase.BaseSpecification

import java.time.Instant

class OutboxEventListenerSpec extends BaseSpecification {

    OutboxEvent outboxEvent = Mock()
    ObjectMapper objectMapper = Mock()

    OutboxEventListener listener = new OutboxEventListener(outboxEvent, objectMapper)

    def "test onDomainEvent - Should persist event to outbox table"() {
        given:
        Instant occurredAt = Instant.now()
        DomainEvent event = new DomainEvent("CREATED", "PaymentVO", 42, new PaymentVO(), occurredAt)
        String serializedPayload = '{"paymentID":null}'
        OutboxEventVO capturedVO

        when: "The target method executed"
        listener.onDomainEvent(event)

        then: "The expected calls are made"
        1 * objectMapper.writeValueAsString(_ as PaymentVO) >> serializedPayload
        1 * outboxEvent.save(_ as OutboxEventVO) >> { OutboxEventVO vo -> capturedVO = vo }
        0 * _

        and: "The outbox event is correctly populated"
        capturedVO != null
        capturedVO.aggregateType == "PaymentVO"
        capturedVO.aggregateId == 42
        capturedVO.eventType == "CREATED"
        capturedVO.payload == serializedPayload
        capturedVO.createdAt == occurredAt
        noExceptionThrown()
    }

    def "test onDomainEvent - Should handle JsonProcessingException gracefully"() {
        given:
        DomainEvent event = new DomainEvent("UPDATED", "MemberVO", 1, null, Instant.now())

        when: "The target method executed"
        listener.onDomainEvent(event)

        then: "Serialization fails but no exception propagates"
        1 * objectMapper.writeValueAsString(_) >> { throw new JsonProcessingException("test error") {} }
        0 * _

        and: "The exception is caught gracefully"
        noExceptionThrown()
    }
}