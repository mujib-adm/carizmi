package io.carizmi.domain.membership.listener;

import io.carizmi.domain.finance.model.PaymentVO;
import io.carizmi.domain.membership.service.Member;
import io.carizmi.framework.event.AbstractDomainEventListener;
import io.carizmi.framework.event.DomainEvent;
import io.carizmi.shared.constants.ReferenceConstants;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Synchronous in-transaction domain event listener for member activation upon registration fee receipt.
 *
 * <p>Extends {@link AbstractDomainEventListener} to adhere to the platform's standard
 * event-driven architecture, listening for {@link PaymentVO} domain events published
 * automatically by {@link io.carizmi.framework.bl.AbstractBusinessLogic} within the active transaction.</p>
 */
@Component
@RequiredArgsConstructor
public class MemberActivationListener extends AbstractDomainEventListener {

    private static final Logger logger = LoggerFactory.getLogger(MemberActivationListener.class);
    private static final Set<String> SUPPORTED_TYPES = Set.of("PaymentVO");

    private final Member member;

    @Override
    protected Logger getLogger() {
        return logger;
    }

    @Override
    protected Set<String> supportedAggregateTypes() {
        return SUPPORTED_TYPES;
    }

    @Override
    protected void onEvent(DomainEvent<?> event) {
        if (event.payload() instanceof PaymentVO paymentVO) {
            if (ReferenceConstants.FEE_TYPE.REGISTRATION_FEE.equals(paymentVO.getFeeType())) {
                Integer memberID = paymentVO.getMember() != null ? paymentVO.getMember().getMemberID() : null;
                if (memberID != null && paymentVO.getDateReceived() != null) {
                    if ("CREATED".equals(event.eventType())) {
                        logger.info("Activating member {} on CREATED registration fee payment {}", memberID, event.aggregateId());
                        member.activateMemberOnRegistrationFee(memberID, paymentVO.getDateReceived());
                    } else if ("UPDATED".equals(event.eventType())) {
                        logger.info("Updating member {} join date on UPDATED registration fee payment {}", memberID, event.aggregateId());
                        member.updateMemberJoinDateOnRegistrationFee(memberID, paymentVO.getDateReceived());
                    }
                }
            }
        }
    }
}