package org.sofumar.portal.core.businesslogic.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sofumar.portal.constants.FieldConstants;
import org.sofumar.portal.constants.MessagesConstants;
import org.sofumar.portal.constants.ReferenceCodeConstants;
import org.sofumar.portal.core.vo.PaymentVO;
import org.sofumar.portal.data.dto.response.LatestPaymentDto;
import org.sofumar.portal.data.dto.PaymentDto;
import org.sofumar.portal.data.dto.request.PaymentSearchRequestDto;
import org.sofumar.portal.data.dto.response.PaymentSummary;
import org.sofumar.portal.data.transformer.PaymentDtoTransformer;
import org.sofumar.portal.data.transformer.PaymentVOTransformer;
import org.sofumar.portal.framework.data.response.GlobalResponse;
import org.sofumar.portal.framework.data.response.PaginationMeta;
import org.sofumar.portal.framework.exception.RecordNotFoundException;
import org.sofumar.portal.framework.exception.ValidationException;
import org.sofumar.portal.framework.util.ResponseUtils;
import org.sofumar.portal.core.repo.PaymentRepository;
import org.sofumar.portal.core.repo.jpaspec.PaymentSpecifications;
import org.sofumar.portal.core.businesslogic.Payment;
import org.sofumar.portal.service.validation.PaymentValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.sofumar.portal.constants.MessagesConstants.RECORD_ADDED;
import static org.sofumar.portal.constants.MessagesConstants.RECORD_DELETED;
import static org.sofumar.portal.constants.MessagesConstants.RECORD_UPDATED;

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
    @Transactional
    public ResponseEntity<GlobalResponse<Integer>> addPayment(PaymentDto requestDto) {
        // Duplicate Check logic
        if (ReferenceCodeConstants.FEE_TYPE.MEMBERSHIP_FEE.equalsIgnoreCase(requestDto.getFeeType())) {
            validateDuplicate(requestDto);
        }

        PaymentVO vo = voTransformer.transform(requestDto);
        validator.validate(vo);
        PaymentVO savedPayment = add(vo);
        return ResponseUtils.okWithData(savedPayment.getPaymentID(), RECORD_ADDED.addMessageArgs("Payment").getMessageString());
    }

    @Override
    public ResponseEntity<GlobalResponse<List<LatestPaymentDto>>> getLatestPayments(int limit) {
        logger.info("Fetching {} latest payments", limit);
        PageRequest pageRequest = PageRequest.of(0, limit,
                Sort.by(Sort.Direction.DESC, FieldConstants.DATE_RECEIVED, FieldConstants.PAYMENT_ID));

        List<LatestPaymentDto> latest = getRepo().findAll(pageRequest).getContent().stream()
                .map(p -> LatestPaymentDto.builder()
                        .paymentID(p.getPaymentID())
                        .memberID(p.getMember().getMemberID())
                        .memberName(p.getMember().getFirstName() + " " + p.getMember().getLastName())
                        .feeType(p.getFeeType())
                        .amount(p.getAmount())
                        .paymentDate(p.getDateReceived())
                        .build())
                .toList();

        return ResponseUtils.okWithData(latest);
    }

    @Override
    @Transactional
    public ResponseEntity<GlobalResponse<Void>> updatePayment(PaymentDto requestDto) {
        logger.info("Updating payment: {}", requestDto.getPaymentID());

        PaymentVO existing = getRepo().findById(requestDto.getPaymentID())
                .orElseThrow(() -> new RecordNotFoundException("Payment not found: " + requestDto.getPaymentID()));

        PaymentVO updated = voTransformer.transformForUpdate(requestDto, existing);
        validator.validateForUpdate(updated);
        update(updated);
        return ResponseUtils.ok(RECORD_UPDATED.addMessageArgs("Payment").getMessageString());
    }

    @Override
    @Transactional
    public ResponseEntity<GlobalResponse<Void>> deletePayment(Integer paymentID) {
        PaymentVO existing = getRepo().findById(paymentID)
                .orElseThrow(() -> new RecordNotFoundException("Payment not found: " + paymentID));
        delete(existing);
        return ResponseUtils.ok(RECORD_DELETED.addMessageArgs("Payment").getMessageString());
    }

    @Override
    public ResponseEntity<GlobalResponse<PaymentDto>> getPayment(Integer paymentID) {
        PaymentVO existing = getRepo().findById(paymentID)
                .orElseThrow(() -> new RecordNotFoundException("Payment not found: " + paymentID));
        return ResponseUtils.okWithData(dtoTransformer.transform(existing));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<GlobalResponse<List<PaymentDto>>> searchPayments(PaymentSearchRequestDto request) {

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

        return ResponseUtils.okWithDataPageable(dtoTransformer.transformList(result.toList()), meta);
    }

    @Override
    public BigDecimal sumAmountByDateReceivedBetween(LocalDate start, LocalDate end) {
        return getRepo().sumAmountByDateReceivedBetween(start, end);
    }

    @Override
    public BigDecimal sumAmountByYearAndQuarter(Integer year, Integer quarter) {
        return getRepo().sumAmountByYearAndQuarter(year, quarter);
    }

    @Override
    public BigDecimal sumAmountByMemberID(Integer memberID) {
        return getRepo().sumAmountByMemberID(memberID);
    }

    @Override
    public BigDecimal sumAmountByMemberIDAndYearAndQuarter(Integer memberID, Integer year, Integer quarter) {
        return getRepo().sumAmountByMemberIDAndYearAndQuarter(memberID, year, quarter);
    }

    @Override
    public List<PaymentSummary> findMemberPaymentSummaries(Integer memberID, String feeType) {
        return getRepo().findMemberPaymentSummaries(memberID, feeType);
    }

    @Override
    public List<PaymentSummary> findPaymentSummaries(String feeType, Integer year) {
        return getRepo().findPaymentSummaries(feeType, year);
    }

    @Override
    public List<PaymentVO> findPaymentsByCriteria(Specification<PaymentVO> spec) {
        return getRepo().findAll(spec);
    }

    @Override
    public BigDecimal sumAmountByDateReceivedBefore(LocalDate date) {
        return getRepo().sumAmountByDateReceivedBefore(date);
    }

    private void validateDuplicate(PaymentDto dto) {
        if (checkExists(dto.getMemberID(), dto.getFeeType(), dto.getYear(), dto.getQuarter())) {
            PaymentVO vo = new PaymentVO();
            vo.addFieldMessage(FieldConstants.QUARTER, MessagesConstants.ERR_PAYMENT_ALREADY_EXISTS.addMessageArgs(
                    String.valueOf(dto.getYear()), dto.getQuarter()));
            throw new ValidationException(vo);
        }
    }

    private boolean checkExists(Integer memberID, String feeType, Integer year, Integer quarter) {
        Specification<PaymentVO> spec = Specification.allOf(
                PaymentSpecifications.hasMemberID(memberID),
                PaymentSpecifications.hasFeeType(feeType),
                PaymentSpecifications.hasYear(year),
                PaymentSpecifications.hasQuarter(quarter));
        return getRepo().exists(spec);
    }

    @Override
    public List<PaymentVO> findPaymentsForMemberQuarter(Integer memberID, Integer year, Integer quarter, String feeType) {
        Specification<PaymentVO> spec = Specification.allOf(
                PaymentSpecifications.hasMemberID(memberID),
                PaymentSpecifications.hasYear(year),
                PaymentSpecifications.hasQuarter(quarter),
                PaymentSpecifications.hasFeeType(feeType));
        return getRepo().findAll(spec);
    }
}