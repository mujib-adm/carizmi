package org.sofumar.portal.core.businesslogic.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sofumar.portal.constants.FieldConstants;
import org.sofumar.portal.message.ValidationMessages;
import org.sofumar.portal.constants.ReferenceConstants;
import org.sofumar.portal.core.businesslogic.Payment;
import org.sofumar.portal.core.repo.PaymentRepository;
import org.sofumar.portal.core.repo.jpaspec.PaymentSpecifications;
import org.sofumar.portal.core.vo.PaymentVO;
import org.sofumar.portal.data.dto.PaymentDto;
import org.sofumar.portal.data.dto.request.PaymentSearchRequestDto;
import org.sofumar.portal.data.dto.response.LatestPaymentDto;
import org.sofumar.portal.data.dto.response.PaymentSummary;
import org.sofumar.portal.data.transformer.PaymentDtoTransformer;
import org.sofumar.portal.data.transformer.PaymentVOTransformer;
import org.sofumar.portal.framework.data.response.PagedResult;
import org.sofumar.portal.framework.data.response.PaginationMeta;
import org.sofumar.portal.framework.exception.RecordNotFoundException;
import org.sofumar.portal.service.validation.PaymentValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.sofumar.portal.message.ValidationMessages.RECORD_NOT_FOUND;

@Service
public non-sealed class PaymentImpl extends PaymentAbstractBL implements Payment {

    private static final Logger logger = LoggerFactory.getLogger(PaymentImpl.class);

    private final PaymentVOTransformer voTransformer;
    private final PaymentDtoTransformer dtoTransformer;
    private final PaymentValidator validator;

    @Autowired
    public PaymentImpl(PaymentRepository paymentRepo, PaymentVOTransformer voTransformer,
                       PaymentDtoTransformer dtoTransformer, PaymentValidator validator) {
        super(paymentRepo);
        this.voTransformer = voTransformer;
        this.dtoTransformer = dtoTransformer;
        this.validator = validator;
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }

    @Override
    protected void performDomainValidation(PaymentVO vo, boolean isUpdate) {
        if (isUpdate) {
            validator.validateForUpdate(vo);
        } else {
            validator.validate(vo);
        }
        performStatefulValidation(vo);
    }

    @Override
    @Transactional
    public Integer addPayment(PaymentDto requestDto) {
        PaymentVO vo = voTransformer.transform(requestDto);
        PaymentVO savedPayment = add(vo);
        return savedPayment.getPaymentID();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LatestPaymentDto> getLatestPayments(int limit) {
        logger.info("Fetching {} latest payments", limit);
        PageRequest pageRequest = PageRequest.of(0, limit,
                Sort.by(Sort.Direction.DESC, FieldConstants.DATE_RECEIVED, FieldConstants.PAYMENT_ID));

        return getRepo().findAll(pageRequest).getContent().stream()
                .map(p -> LatestPaymentDto.builder()
                        .paymentID(p.getPaymentID())
                        .memberID(p.getMember().getMemberID())
                        .memberName(p.getMember().getFirstName() + " " + p.getMember().getLastName())
                        .feeType(p.getFeeType())
                        .amount(p.getAmount())
                        .paymentDate(p.getDateReceived())
                        .build())
                .toList();
    }

    @Override
    @Transactional
    public void updatePayment(PaymentDto requestDto) {
        logger.info("Updating payment: {}", requestDto.getPaymentID());

        PaymentVO existingVO = getRepo().findById(requestDto.getPaymentID())
                .orElseThrow(() -> new RecordNotFoundException(RECORD_NOT_FOUND.getMessageText()));

        PaymentVO updatedVO = voTransformer.transformForUpdate(requestDto, existingVO);
        update(updatedVO);
    }

    @Override
    @Transactional
    public void deletePayment(@NonNull Integer paymentID) {
        PaymentVO existingVO = getRepo().findById(paymentID)
                .orElseThrow(() -> new RecordNotFoundException(RECORD_NOT_FOUND.getMessageText()));
        delete(existingVO);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentDto getPayment(@NonNull Integer paymentID) {
        PaymentVO existingVO = getRepo().findById(paymentID)
                .orElseThrow(() -> new RecordNotFoundException(RECORD_NOT_FOUND.getMessageText()));
        return dtoTransformer.transform(existingVO);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResult<PaymentDto> searchPayments(PaymentSearchRequestDto request) {

        List<Specification<PaymentVO>> specs = new ArrayList<>();
        if (request.getMemberID() != null)
            specs.add(PaymentSpecifications.hasMemberID(request.getMemberID()));
        if (request.getFeeType() != null)
            specs.add(PaymentSpecifications.hasFeeType(request.getFeeType()));
        if (request.getYear() != null)
            specs.add(PaymentSpecifications.hasYear(request.getYear()));
        if (request.getQuarter() != null)
            specs.add(PaymentSpecifications.hasQuarter(request.getQuarter()));
        if (request.getDateFrom() != null || request.getDateTo() != null)
            specs.add(PaymentSpecifications.dateReceivedBetween(request.getDateFrom(), request.getDateTo()));

        Specification<PaymentVO> spec = Specification.allOf(specs);
        Page<PaymentVO> result = getRepo().findAll(spec, request.toPageable());
        PaginationMeta meta = PaginationMeta.of(result.getNumber(), result.getSize(), result.getTotalElements(),
                result.getTotalPages());

        return PagedResult.of(dtoTransformer.transformList(result.toList()), meta);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal sumAmountByDateReceivedBetween(LocalDate start, LocalDate end) {
        return getRepo().sumAmountByDateReceivedBetween(start, end);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal sumAmountByYearAndQuarter(@NonNull Integer year, @NonNull Integer quarter) {
        return getRepo().sumAmountByYearAndQuarter(year, quarter);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal sumAmountByMemberID(@NonNull Integer memberID) {
        return getRepo().sumAmountByMemberID(memberID);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal sumAmountByMemberIDAndYearAndQuarter(@NonNull Integer memberID, @NonNull Integer year, @NonNull Integer quarter) {
        return getRepo().sumAmountByMemberIDAndYearAndQuarter(memberID, year, quarter);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentSummary> findMemberPaymentSummaries(@NonNull Integer memberID, String feeType) {
        return getRepo().findMemberPaymentSummaries(memberID, feeType);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentSummary> findMembersPaymentSummaries(List<Integer> memberIds, String feeType, @NonNull Integer year) {
        return getRepo().findMembersPaymentSummaries(memberIds, feeType, year);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentSummary> findPaymentSummaries(String feeType, @NonNull Integer year) {
        return getRepo().findPaymentSummaries(feeType, year);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentVO> findPaymentsByCriteria(Specification<PaymentVO> spec) {
        return getRepo().findAll(spec);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal sumAmountByDateReceivedBefore(LocalDate date) {
        return getRepo().sumAmountByDateReceivedBefore(date);
    }

    private void performStatefulValidation(PaymentVO vo) {
        // Duplicate Check logic for payments (add & update)
        if (ReferenceConstants.FEE_TYPE.MEMBERSHIP_FEE.equals(vo.getFeeType())) {
            if (checkExists(vo.getPaymentID(), vo.getMember().getMemberID(), vo.getFeeType(), vo.getYear(), vo.getQuarter())) {
                vo.addFieldMessage(FieldConstants.QUARTER, ValidationMessages.ERR_PAYMENT_ALREADY_EXISTS.addMessageArgs(
                        String.valueOf(vo.getYear()), vo.getQuarter()));
            }
        }
    }

    private boolean checkExists(Integer paymentID, Integer memberID, String feeType, Integer year, Integer quarter) {
        Specification<PaymentVO> spec = Specification.allOf(
                PaymentSpecifications.hasMemberID(memberID),
                PaymentSpecifications.hasFeeType(feeType),
                PaymentSpecifications.hasYear(year),
                PaymentSpecifications.hasQuarter(quarter),
                PaymentSpecifications.notPaymentId(paymentID));
        return getRepo().exists(spec);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentVO> findPaymentsForMemberQuarter(@NonNull Integer memberID, @NonNull Integer year, @NonNull Integer quarter, String feeType) {
        Specification<PaymentVO> spec = Specification.allOf(
                PaymentSpecifications.hasMemberID(memberID),
                PaymentSpecifications.hasYear(year),
                PaymentSpecifications.hasQuarter(quarter),
                PaymentSpecifications.hasFeeType(feeType));
        return getRepo().findAll(spec);
    }
}