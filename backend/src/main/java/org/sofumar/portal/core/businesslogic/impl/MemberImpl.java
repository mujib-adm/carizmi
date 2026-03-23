package org.sofumar.portal.core.businesslogic.impl;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sofumar.portal.constants.FieldConstants;
import org.sofumar.portal.constants.ReferenceConstants;
import org.sofumar.portal.core.businesslogic.Member;
import org.sofumar.portal.core.businesslogic.Payment;
import org.sofumar.portal.core.repo.MemberRepository;
import org.sofumar.portal.core.repo.jpaspec.MemberSpecifications;
import org.sofumar.portal.core.vo.MemberVO;
import org.sofumar.portal.data.dto.MemberDto;
import org.sofumar.portal.data.dto.request.MemberSearchRequestDto;
import org.sofumar.portal.data.dto.response.MemberLookupDto;
import org.sofumar.portal.data.dto.response.MemberSummaryDto;
import org.sofumar.portal.data.dto.response.PaymentSummary;
import org.sofumar.portal.data.transformer.MemberDtoTransformer;
import org.sofumar.portal.data.transformer.MemberVOTransformer;
import org.sofumar.portal.framework.data.response.GlobalResponse;
import org.sofumar.portal.framework.data.response.PaginationMeta;
import org.sofumar.portal.framework.exception.RecordNotFoundException;
import org.sofumar.portal.framework.util.ResponseUtils;
import org.sofumar.portal.service.validation.MemberValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.sofumar.portal.message.ValidationMessages.RECORD_ADDED;
import static org.sofumar.portal.message.ValidationMessages.RECORD_DELETED;
import static org.sofumar.portal.message.ValidationMessages.RECORD_NOT_FOUND;
import static org.sofumar.portal.message.ValidationMessages.RECORD_UPDATED;

@Service
public non-sealed class MemberImpl extends MemberAbstractBL implements Member {
    private static final Logger logger = LoggerFactory.getLogger(MemberImpl.class);

    private static final BigDecimal quarterlyFeeAmt = new BigDecimal("60");

    private final Payment payment;
    private final MemberVOTransformer voTransformer;
    private final MemberDtoTransformer dtoTransformer;
    private final MemberValidator validator;

    @Autowired
    public MemberImpl(final MemberRepository memberRepo, final Payment payment, final MemberVOTransformer voTransformer, MemberDtoTransformer dtoTransformer, final MemberValidator validator) {
        super(memberRepo);
        this.payment = payment;
        this.voTransformer = voTransformer;
        this.dtoTransformer = dtoTransformer;
        this.validator = validator;
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }

    @Override
    protected void performDomainValidation(MemberVO vo, boolean isUpdate) {
        if (isUpdate) {
            validator.validateForUpdate(vo);
        } else {
            validator.validate(vo);
        }
    }

    @Override
    @Transactional
    public ResponseEntity<GlobalResponse<Integer>> addMember(MemberDto requestDto) {
        MemberVO memberVO = voTransformer.transform(requestDto);
        MemberVO savedMember = add(memberVO);
        logger.info("Member added successfully with ID: {}", savedMember.getMemberID());
        return ResponseUtils.okWithData(savedMember.getMemberID(), RECORD_ADDED.addMessageArgs("Member").getMessageString());
    }

    @Override
    @Transactional
    public ResponseEntity<GlobalResponse<Void>> updateMember(MemberDto requestDto) {
        MemberVO existingMember = getRepo().findById(requestDto.getMemberID())
                .orElseThrow(() -> new RecordNotFoundException(RECORD_NOT_FOUND.getMessageText()));

        MemberVO updatedMember = voTransformer.transformForUpdate(requestDto, existingMember);
        MemberVO savedMember = update(updatedMember);
        logger.info("Member updated successfully, memberID: {}", savedMember.getMemberID());
        return ResponseUtils.ok(RECORD_UPDATED.addMessageArgs("Member").getMessageString());
    }

    @Override
    @Transactional
    public ResponseEntity<GlobalResponse<Void>> deleteMember(Integer memberID) {
        MemberVO existingMember = getRepo().findById(memberID)
                .orElseThrow(() -> new RecordNotFoundException(RECORD_NOT_FOUND.getMessageText()));

        delete(existingMember);
        logger.info("Member deleted successfully, memberID: {}", memberID);
        return ResponseUtils.ok(RECORD_DELETED.addMessageArgs("Member").getMessageString());
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<GlobalResponse<List<MemberDto>>> searchMembers(MemberSearchRequestDto request) {
        List<Specification<MemberVO>> specList = new ArrayList<>();
        if (StringUtils.isNotBlank(request.getFirstName()))
            specList.add(MemberSpecifications.hasFirstName(request.getFirstName()));
        if (StringUtils.isNotBlank(request.getLastName()))
            specList.add(MemberSpecifications.hasLastName(request.getLastName()));
        if (StringUtils.isNotBlank(request.getPhone()))
            specList.add(MemberSpecifications.hasPhone(request.getPhone()));
        if (StringUtils.isNotBlank(request.getStatus()))
            specList.add(MemberSpecifications.hasState(request.getStatus()));

        Specification<MemberVO> spec = Specification.allOf(specList);
        Page<MemberVO> members = getRepo().findAll(spec, request.toPageable());
        PaginationMeta meta = PaginationMeta.of(members.getNumber(), members.getSize(), members.getTotalElements(), members.getTotalPages());
        return ResponseUtils.okWithDataPageable(dtoTransformer.transformList(members.toList()), meta);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<GlobalResponse<MemberDto>> getMember(Integer memberID) {
        MemberVO member = getRepo().findById(memberID)
                .orElseThrow(() -> new RecordNotFoundException(RECORD_NOT_FOUND.getMessageText()));

        return ResponseUtils.okWithData(dtoTransformer.transform(member));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<GlobalResponse<List<MemberLookupDto>>> lookupMembers(String query) {
        Specification<MemberVO> spec = MemberSpecifications.lookup(query);
        PageRequest pageRequest = PageRequest.of(0, 10, Sort.by(FieldConstants.FIRST_NAME, FieldConstants.LAST_NAME));
        Page<MemberVO> members = getRepo().findAll(spec, pageRequest);

        List<MemberLookupDto> dtos = members.getContent().stream()
                .map(memberVO -> MemberLookupDto.builder()
                        .memberID(memberVO.getMemberID())
                        .firstName(memberVO.getFirstName())
                        .lastName(memberVO.getLastName())
                        .phone(memberVO.getPhone())
                        .status(memberVO.getStatus())
                        .build())
                .collect(Collectors.toList());

        return ResponseUtils.okWithData(dtos);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<GlobalResponse<MemberSummaryDto>> getMemberSummary(@NonNull Integer memberID) {

        Optional<MemberVO> memberOpt = getRepo().findById(memberID);
        if (memberOpt.isEmpty()) {
            return ResponseUtils.okWithData(MemberSummaryDto.builder()
                    .totalPaid(BigDecimal.ZERO)
                    .outstanding(BigDecimal.ZERO)
                    .overdue(BigDecimal.ZERO)
                    .build());
        }

        MemberVO member = memberOpt.get();

        LocalDate now = LocalDate.now();
        int currentYear = now.getYear();
        int currentQuarter = (now.getMonthValue() - 1) / 3 + 1;

        // 1. Total Paid
        BigDecimal totalPaid = payment.sumAmountByMemberID(memberID);
        if (totalPaid == null) totalPaid = BigDecimal.ZERO;

        // 2. Outstanding (Current Quarter)
        BigDecimal collectedCurrentQ = payment.sumAmountByMemberIDAndYearAndQuarter(memberID, currentYear, currentQuarter);
        if (collectedCurrentQ == null) collectedCurrentQ = BigDecimal.ZERO;

        BigDecimal outstanding = quarterlyFeeAmt.subtract(collectedCurrentQ);
        if (outstanding.compareTo(BigDecimal.ZERO) < 0) outstanding = BigDecimal.ZERO;

        // 3. Overdue (Past Quarters since Join Date)
        List<PaymentSummary> summaries = payment.findMemberPaymentSummaries(memberID, ReferenceConstants.FEE_TYPE.MEMBERSHIP_FEE);
        Map<String, BigDecimal> paymentMap = new HashMap<>(); // key: "YYYY-Q"
        for (PaymentSummary s : summaries) {
            String key = s.getYear() + "-" + s.getQuarter();
            paymentMap.put(key, s.getTotalPaid());
        }

        BigDecimal overdue = BigDecimal.ZERO;
        LocalDate joinDate = member.getJoinDate();
        if (joinDate == null) joinDate = LocalDate.now();

        // Iterate through all years and quarters since joinDate up to the quarter BEFORE the current one
        int joinYear = joinDate.getYear();
        int joinQuarter = (joinDate.getMonthValue() - 1) / 3 + 1;

        for (int y = joinYear; y <= currentYear; y++) {
            int startQ = (y == joinYear) ? joinQuarter : 1;
            int endQ = (y == currentYear) ? currentQuarter - 1 : 4;

            for (int q = startQ; q <= endQ; q++) {
                String key = y + "-" + q;
                BigDecimal paid = paymentMap.getOrDefault(key, BigDecimal.ZERO);
                BigDecimal due = quarterlyFeeAmt.subtract(paid);
                if (due.compareTo(BigDecimal.ZERO) > 0) {
                    overdue = overdue.add(due);
                }
            }
        }

        MemberSummaryDto summary = MemberSummaryDto.builder()
                .totalPaid(totalPaid)
                .outstanding(outstanding)
                .overdue(overdue)
                .build();

        return ResponseUtils.okWithData(summary);
    }

    @Override
    @Transactional(readOnly = true)
    public long countActiveMembers() {
        return getRepo().count(MemberSpecifications.hasStatus(ReferenceConstants.MEMBER_STATUS.ACTIVE));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MemberVO> findAllActiveMembers() {
        return getRepo().findAll(MemberSpecifications.hasStatus(ReferenceConstants.MEMBER_STATUS.ACTIVE));
    }
}