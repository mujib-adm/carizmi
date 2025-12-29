package org.sofumar.portal.service.businesslogic.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sofumar.portal.constants.FieldConstants;
import org.sofumar.portal.constants.MessagesConstants;
import org.sofumar.portal.constants.ReferenceCodeConstants;
import org.sofumar.portal.data.dto.PaymentDto;
import org.sofumar.portal.data.transformer.PaymentDtoTransformer;
import org.sofumar.portal.data.transformer.PaymentVOTransformer;
import org.sofumar.portal.data.vo.PaymentVO;
import org.sofumar.portal.framework.bl.AbstractBusinessLogic;
import org.sofumar.portal.framework.data.response.GlobalResponse;
import org.sofumar.portal.framework.data.response.PaginationMeta;
import org.sofumar.portal.framework.exception.RecordNotFoundException;
import org.sofumar.portal.framework.exception.ValidationException;
import org.sofumar.portal.framework.util.ResponseUtils;
import org.sofumar.portal.repo.PaymentRepository;
import org.sofumar.portal.repo.jpaspec.PaymentSpecifications;
import org.sofumar.portal.service.businesslogic.PaymentService;
import org.sofumar.portal.service.validation.PaymentValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.sofumar.portal.constants.MessagesConstants.RECORD_ADDED;
import static org.sofumar.portal.constants.MessagesConstants.RECORD_DELETED;
import static org.sofumar.portal.constants.MessagesConstants.RECORD_UPDATED;

@Service
public class PaymentServiceImpl extends AbstractBusinessLogic<PaymentVO, PaymentRepository> implements PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentServiceImpl.class);

    private final PaymentRepository paymentRepo;
    private final PaymentVOTransformer voTransformer;
    private final PaymentDtoTransformer dtoTransformer;
    private final PaymentValidator validator;

    @Autowired
    public PaymentServiceImpl(PaymentRepository paymentRepo, PaymentVOTransformer voTransformer,
                              PaymentDtoTransformer dtoTransformer, PaymentValidator validator) {
        this.paymentRepo = paymentRepo;
        this.voTransformer = voTransformer;
        this.dtoTransformer = dtoTransformer;
        this.validator = validator;
    }

    @Override
    protected PaymentRepository getRepository() {
        return paymentRepo;
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }

    @Override
    @Transactional
    public ResponseEntity<GlobalResponse<Void>> addPayment(PaymentDto requestDto) {
        // Duplicate Check logic
        if (ReferenceCodeConstants.FEE_TYPE.MEMBERSHIP_FEE.equalsIgnoreCase(requestDto.getFeeType())) {
            validateDuplicate(requestDto);
        }

        PaymentVO vo = voTransformer.transform(requestDto);
        validator.validate(vo);
        add(vo);
        return ResponseUtils.ok(RECORD_ADDED.addMessageArgs("Payment").getMessageString());
    }

    @Override
    @Transactional
    public ResponseEntity<GlobalResponse<Void>> updatePayment(PaymentDto requestDto) {
        logger.info("Updating payment: {}", requestDto.getPaymentID());

        PaymentVO existing = paymentRepo.findById(requestDto.getPaymentID())
                .orElseThrow(() -> new RecordNotFoundException("Payment not found: " + requestDto.getPaymentID()));

        // If critical fields change, re-check duplicate? usually updates are allowed to
        // correct data.
        // Assuming strict duplicate check only on creation or if completely changing
        // the period.

        PaymentVO updated = voTransformer.transformForUpdate(requestDto, existing);
        validator.validateForUpdate(updated);
        update(updated);
        return ResponseUtils.ok(RECORD_UPDATED.addMessageArgs("Payment").getMessageString());
    }

    @Override
    @Transactional
    public ResponseEntity<GlobalResponse<Void>> deletePayment(Integer paymentID) {
        PaymentVO existing = paymentRepo.findById(paymentID)
                .orElseThrow(() -> new RecordNotFoundException("Payment not found: " + paymentID));
        delete(existing);
        return ResponseUtils.ok(RECORD_DELETED.addMessageArgs("Payment").getMessageString());
    }

    @Override
    public ResponseEntity<GlobalResponse<PaymentDto>> getPayment(Integer paymentID) {
        PaymentVO existing = paymentRepo.findById(paymentID)
                .orElseThrow(() -> new RecordNotFoundException("Payment not found: " + paymentID));
        return ResponseUtils.okWithData(dtoTransformer.transform(existing));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<GlobalResponse<List<PaymentDto>>> searchPayments(
            Integer memberID, String feeType, Integer year, Integer quarter,
            LocalDate dateFrom, LocalDate dateTo,
            int page, int size, String sortField, String sortOrder) {

        List<Specification<PaymentVO>> specs = new ArrayList<>();
        if (memberID != null)
            specs.add(PaymentSpecifications.hasMemberID(memberID));
        if (feeType != null)
            specs.add(PaymentSpecifications.hasFeeType(feeType));
        if (year != null)
            specs.add(PaymentSpecifications.hasYear(year));
        if (quarter != null)
            specs.add(PaymentSpecifications.hasQuarter(quarter));
        if (dateFrom != null || dateTo != null)
            specs.add(PaymentSpecifications.dateReceivedBetween(dateFrom, dateTo));

        Specification<PaymentVO> spec = Specification.allOf(specs);
        Sort sort = Sort.unsorted();
        if (sortField != null && sortOrder != null) {
            sort = Sort.by(Sort.Direction.fromString(sortOrder), sortField);
        }
        PageRequest pageRequest = PageRequest.of(page, size, sort);
        Page<PaymentVO> result = paymentRepo.findAll(spec, pageRequest);
        PaginationMeta meta = PaginationMeta.of(result.getNumber(), result.getSize(), result.getTotalElements(),
                result.getTotalPages());

        return ResponseUtils.okWithDataPageable(dtoTransformer.transformList(result.toList()), meta);
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
        return paymentRepo.count(spec) > 0;
    }
}