package io.carizmi.domain.membership.service;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.carizmi.shared.constants.FieldConstants;
import io.carizmi.shared.constants.ReferenceConstants;
import io.carizmi.domain.finance.service.Payment;
import io.carizmi.domain.platform.service.SystemSetting;
import io.carizmi.domain.membership.repository.MemberRepository;
import io.carizmi.domain.membership.repository.spec.MemberSpecifications;
import io.carizmi.domain.membership.model.MemberVO;
import io.carizmi.domain.membership.data.dto.MemberDto;
import io.carizmi.domain.membership.data.dto.request.MemberSearchRequestDto;
import io.carizmi.domain.membership.data.dto.response.MemberLookupDto;
import io.carizmi.shared.data.dto.MemberJoinDateProjection;
import io.carizmi.domain.membership.data.dto.response.MemberSummaryDto;
import io.carizmi.shared.data.dto.PaymentSummary;
import io.carizmi.domain.membership.data.transformer.MemberDtoTransformer;
import io.carizmi.domain.membership.data.transformer.MemberVOTransformer;
import io.carizmi.framework.data.response.PagedResult;
import io.carizmi.framework.data.response.PaginationMeta;
import io.carizmi.framework.exception.RecordNotFoundException;
import io.carizmi.domain.membership.validation.MemberValidator;
import io.carizmi.shared.util.QuarterUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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

import static io.carizmi.shared.message.ValidationMessages.RECORD_NOT_FOUND;

@Service
public non-sealed class MemberImpl extends MemberAbstractBL implements Member {
    private static final Logger logger = LoggerFactory.getLogger(MemberImpl.class);

    private final Payment payment;
    private final SystemSetting systemSetting;
    private final MemberVOTransformer voTransformer;
    private final MemberDtoTransformer dtoTransformer;
    private final MemberValidator validator;

    @Autowired
    public MemberImpl(final MemberRepository memberRepo, final Payment payment, final SystemSetting systemSetting,
                      final MemberVOTransformer voTransformer, MemberDtoTransformer dtoTransformer,
                      final MemberValidator validator) {
        super(memberRepo);
        this.payment = payment;
        this.systemSetting = systemSetting;
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
    public Integer addMember(MemberDto requestDto) {
        MemberVO memberVO = voTransformer.transform(requestDto);
        MemberVO savedMember = add(memberVO);
        return savedMember.getMemberID();
    }

    @Override
    @Transactional
    public void updateMember(MemberDto requestDto) {
        MemberVO existingMember = getRepo().findById(requestDto.getMemberID())
                .orElseThrow(() -> new RecordNotFoundException(RECORD_NOT_FOUND.getMessageText()));

        MemberVO updatedVO = voTransformer.transformForUpdate(requestDto, existingMember);
        update(updatedVO);
    }

    @Override
    @Transactional
    public void deleteMember(Integer memberID) {
        MemberVO existingMember = getRepo().findById(memberID)
                .orElseThrow(() -> new RecordNotFoundException(RECORD_NOT_FOUND.getMessageText()));

        delete(existingMember);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResult<MemberDto> searchMembers(MemberSearchRequestDto request) {
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
        return PagedResult.of(dtoTransformer.transformList(members.toList()), meta);
    }

    @Override
    @Transactional(readOnly = true)
    public MemberDto getMember(Integer memberID) {
        MemberVO member = getRepo().findById(memberID)
                .orElseThrow(() -> new RecordNotFoundException(RECORD_NOT_FOUND.getMessageText()));

        return dtoTransformer.transform(member);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MemberLookupDto> lookupMembers(String query) {
        Specification<MemberVO> spec = MemberSpecifications.lookup(query);
        PageRequest pageRequest = PageRequest.of(0, 10, Sort.by(FieldConstants.FIRST_NAME, FieldConstants.LAST_NAME));
        Page<MemberVO> members = getRepo().findAll(spec, pageRequest);

        return members.getContent().stream()
                .map(memberVO -> MemberLookupDto.builder()
                        .memberID(memberVO.getMemberID())
                        .firstName(memberVO.getFirstName())
                        .lastName(memberVO.getLastName())
                        .phone(memberVO.getPhone())
                        .status(memberVO.getStatus())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public MemberSummaryDto getMemberSummary(@NonNull Integer memberID) {

        Optional<MemberVO> memberOpt = getRepo().findById(memberID);
        if (memberOpt.isEmpty()) {
            return MemberSummaryDto.builder()
                    .totalPaid(BigDecimal.ZERO)
                    .outstanding(BigDecimal.ZERO)
                    .overdue(BigDecimal.ZERO)
                    .build();
        }

        MemberVO member = memberOpt.get();

        LocalDate now = LocalDate.now();
        int currentYear = now.getYear();
        int currentQuarter = QuarterUtils.quarterOf(now);

        BigDecimal quarterlyFeeAmt = systemSetting.getQuarterlyFeeAmount();

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
        LocalDate joinDate = QuarterUtils.resolveJoinDate(member.getJoinDate());

        // Iterate through all years and quarters since joinDate up to the quarter BEFORE the current one
        int joinYear = joinDate.getYear();
        int joinQuarter = QuarterUtils.quarterOf(joinDate);

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

        return MemberSummaryDto.builder()
                .totalPaid(totalPaid)
                .outstanding(outstanding)
                .overdue(overdue)
                .build();
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

    @Override
    @Transactional(readOnly = true)
    public Page<MemberVO> findActiveMembers(@NonNull Pageable pageable) {
        return getRepo().findAll(MemberSpecifications.hasStatus(ReferenceConstants.MEMBER_STATUS.ACTIVE), pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MemberJoinDateProjection> findActiveMemberJoinDates() {
        return getRepo().findJoinDatesByStatus(ReferenceConstants.MEMBER_STATUS.ACTIVE);
    }
}