package io.carizmi.domain.membership.listener

import io.carizmi.domain.finance.model.PaymentVO
import io.carizmi.domain.membership.model.MemberVO
import io.carizmi.domain.membership.service.Member
import io.carizmi.framework.event.DomainEvent
import io.carizmi.shared.constants.ReferenceConstants
import io.carizmi.testbase.BaseSpecification
import spock.lang.Subject

import java.time.Instant
import java.time.LocalDate

class MemberActivationListenerSpec extends BaseSpecification {

    Member member = Mock()

    @Subject
    MemberActivationListener listener = new MemberActivationListener(member)

    def "test - supportedAggregateTypes: Returns PaymentVO"() {
        when: "Querying supported aggregate types"
        Set<String> types = listener.supportedAggregateTypes()

        then: "PaymentVO is declared"
        types.size() == 1
        types.contains("PaymentVO")
        noExceptionThrown()
    }

    def "test onDomainEvent: New registration fee CREATED event triggers activateMemberOnRegistrationFee"() {
        given: "A CREATED PaymentVO DomainEvent with feeType = REGISTRATION_FEE"
        Integer memberID = 42
        Integer paymentID = 100
        LocalDate dateReceived = LocalDate.of(2026, 5, 20)
        PaymentVO paymentVO = new PaymentVO(
                paymentID: paymentID,
                member: new MemberVO(memberID: memberID),
                feeType: ReferenceConstants.FEE_TYPE.REGISTRATION_FEE,
                dateReceived: dateReceived
        )
        DomainEvent<PaymentVO> event = new DomainEvent<>("CREATED", "PaymentVO", paymentID, paymentVO, Instant.now())

        when: "Listener receives domain event"
        listener.onDomainEvent(event)

        then: "Member activation is triggered"
        1 * member.activateMemberOnRegistrationFee(memberID, dateReceived)
        0 * _

        and:
        noExceptionThrown()
    }

    def "test onDomainEvent: Updated registration fee UPDATED event triggers updateMemberJoinDateOnRegistrationFee"() {
        given: "An UPDATED PaymentVO DomainEvent with feeType = REGISTRATION_FEE"
        Integer memberID = 42
        Integer paymentID = 100
        LocalDate newDate = LocalDate.of(2026, 7, 1)
        PaymentVO paymentVO = new PaymentVO(
                paymentID: paymentID,
                member: new MemberVO(memberID: memberID),
                feeType: ReferenceConstants.FEE_TYPE.REGISTRATION_FEE,
                dateReceived: newDate
        )
        DomainEvent<PaymentVO> event = new DomainEvent<>("UPDATED", "PaymentVO", paymentID, paymentVO, Instant.now())

        when: "Listener receives domain event"
        listener.onDomainEvent(event)

        then: "Member join date update is triggered"
        1 * member.updateMemberJoinDateOnRegistrationFee(memberID, newDate)
        0 * _

        and:
        noExceptionThrown()
    }

    def "test onDomainEvent: Membership fee payment is ignored"() {
        given: "A CREATED PaymentVO DomainEvent with feeType = MEMBERSHIP_FEE"
        Integer memberID = 42
        Integer paymentID = 100
        PaymentVO paymentVO = new PaymentVO(
                paymentID: paymentID,
                member: new MemberVO(memberID: memberID),
                feeType: ReferenceConstants.FEE_TYPE.MEMBERSHIP_FEE,
                dateReceived: LocalDate.now()
        )
        DomainEvent<PaymentVO> event = new DomainEvent<>("CREATED", "PaymentVO", paymentID, paymentVO, Instant.now())

        when: "Listener receives domain event"
        listener.onDomainEvent(event)

        then: "No member activation calls made"
        0 * _

        and:
        noExceptionThrown()
    }

    def "test onDomainEvent: Unsupported aggregate type is filtered out by framework"() {
        given: "A DomainEvent with an unsupported aggregate type"
        DomainEvent<?> event = new DomainEvent<>("CREATED", "ExpenseVO", 1, null, Instant.now())

        when: "Listener receives domain event"
        listener.onDomainEvent(event)

        then: "Event is filtered out — no calls to member"
        0 * _

        and:
        noExceptionThrown()
    }

    def "test onDomainEvent: Ignores event with null member or null dateReceived"() {
        given: "PaymentVO with null member or null dateReceived"
        PaymentVO nullMemberVO = new PaymentVO(
                paymentID: 1,
                member: null,
                feeType: ReferenceConstants.FEE_TYPE.REGISTRATION_FEE,
                dateReceived: LocalDate.now()
        )
        PaymentVO nullDateVO = new PaymentVO(
                paymentID: 2,
                member: new MemberVO(memberID: 10),
                feeType: ReferenceConstants.FEE_TYPE.REGISTRATION_FEE,
                dateReceived: null
        )
        DomainEvent<PaymentVO> e1 = new DomainEvent<>("CREATED", "PaymentVO", 1, nullMemberVO, Instant.now())
        DomainEvent<PaymentVO> e2 = new DomainEvent<>("CREATED", "PaymentVO", 2, nullDateVO, Instant.now())

        when: "Events are handled"
        listener.onDomainEvent(e1)
        listener.onDomainEvent(e2)

        then: "No calls to member service"
        0 * _

        and:
        noExceptionThrown()
    }
}