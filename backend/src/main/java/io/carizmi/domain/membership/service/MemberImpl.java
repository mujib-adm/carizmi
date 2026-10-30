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
import io.carizmi.domain.membership.model.MemberMembershipPeriodVO;
import io.carizmi.domain.membership.data.dto.MemberDto;
import io.carizmi.domain.membership.data.dto.request.MemberSearchRequestDto;
import io.carizmi.domain.membership.data.dto.response.MemberLookupDto;
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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static io.carizmi.shared.message.ValidationMessages.RECORD_NOT_FOUND;

@Service
public final class MemberImpl extends MemberAbstractBL implements Member {
    private static final Logger logger = LoggerFactory.getLogger(MemberImpl.class);

    private final Payment payment;
    private final SystemSetting systemSetting;
    private final MemberVOTransformer voTransformer;
    private final MemberDtoTransformer dtoTransformer;
    private final MemberValidator validator;
    private final MemberMembershipPeriod memberMembershipPeriod;

    @Autowired
    public MemberImpl(final MemberRepository memberRepo, final Payment payment, final SystemSetting systemSetting,
                      final MemberVOTransformer voTransformer, MemberDtoTransformer dtoTransformer,
                      final MemberValidator validator,
                      final MemberMembershipPeriod memberMembershipPeriod) {
        super(memberRepo);
        this.payment = payment;
        this.systemSetting = systemSetting;
        this.voTransformer = voTransformer;
        this.dtoTransformer = dtoTransformer;
        this.validator = validator;
        this.memberMembershipPeriod = memberMembershipPeriod;
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
    protected void performDeleteValidation(MemberVO vo) {
        validator.validateForDelete(vo);
    }

    @Override
    @Transactional
    public Integer addMember(MemberDto requestDto) {
        MemberVO memberVO = voTransformer.transform(requestDto);
        memberVO.setStatus(ReferenceConstants.MEMBER_STATUS.PENDING);
        memberVO.setJoinDate(null);
        MemberVO savedMember = add(memberVO);
        return savedMember.getMemberID();
    }

    @Override
    @Transactional
    public void updateMember(MemberDto requestDto) {
        MemberVO existingMember = getRepo().findById(requestDto.getMemberID())
                .orElseThrow(() -> new RecordNotFoundException(RECORD_NOT_FOUND.getMessageText()));

        String previousStatus = existingMember.getStatus();
        MemberVO updatedVO = voTransformer.transformForUpdate(requestDto, existingMember);
        update(updatedVO);

        LocalDate transitionDate = requestDto.getReactivationDate() != null ? requestDto.getReactivationDate() : requestDto.getJoinDate();
        handleMembershipPeriodTransitions(updatedVO, previousStatus, transitionDate);
    }

    @Override
    @Transactional
    public void deleteMember(Integer memberID) {
        MemberVO existingMember = getRepo().findById(memberID)
                .orElseThrow(() -> new RecordNotFoundException(RECORD_NOT_FOUND.getMessageText()));

        delete(existingMember);
    }

    private void handleMembershipPeriodTransitions(MemberVO memberVO, String previousStatus, LocalDate specifiedDate) {
        String currentStatus = memberVO.getStatus();
        if (currentStatus == null || currentStatus.equals(previousStatus)) {
            return;
        }

        Integer memberID = memberVO.getMemberID();
        LocalDate actionDate = specifiedDate != null ? specifiedDate : LocalDate.now();

        // 1. Initial Activation: PENDING -> ACTIVE
        if (ReferenceConstants.MEMBER_STATUS.PENDING.equals(previousStatus)
                && ReferenceConstants.MEMBER_STATUS.ACTIVE.equals(currentStatus)) {
            LocalDate start = memberVO.getJoinDate() != null ? memberVO.getJoinDate() : actionDate;
            memberMembershipPeriod.add(MemberMembershipPeriodVO.builder()
                    .memberID(memberID)
                    .startDate(start)
                    .endDate(null)
                    .build());
        }
        // 2. Deactivation: ACTIVE -> INACTIVE
        else if (ReferenceConstants.MEMBER_STATUS.ACTIVE.equals(previousStatus)
                && ReferenceConstants.MEMBER_STATUS.INACTIVE.equals(currentStatus)) {
            Optional<MemberMembershipPeriodVO> openPeriod = memberMembershipPeriod
                    .findFirstByMemberIDAndEndDateIsNullOrderByStartDateDesc(memberID);
            LocalDate leave = actionDate;
            if (openPeriod.isPresent()) {
                MemberMembershipPeriodVO period = openPeriod.get();
                period.setEndDate(leave);
                memberMembershipPeriod.update(period);
            } else {
                LocalDate start = memberVO.getJoinDate() != null ? memberVO.getJoinDate() : leave;
                memberMembershipPeriod.add(MemberMembershipPeriodVO.builder()
                        .memberID(memberID)
                        .startDate(start)
                        .endDate(leave)
                        .build());
            }
        }
        // 3. Reactivation: INACTIVE -> ACTIVE
        else if (ReferenceConstants.MEMBER_STATUS.INACTIVE.equals(previousStatus)
                && ReferenceConstants.MEMBER_STATUS.ACTIVE.equals(currentStatus)) {
            LocalDate reactivationStart = specifiedDate != null ? specifiedDate : LocalDate.now();
            memberMembershipPeriod.add(MemberMembershipPeriodVO.builder()
                    .memberID(memberID)
                    .startDate(reactivationStart)
                    .endDate(null)
                    .build());
        }
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
            specList.add(MemberSpecifications.hasStatus(request.getStatus()));

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

        // 2. Outstanding (Current Quarter)
        BigDecimal collectedCurrentQ = payment.sumAmountByMemberIDAndYearAndQuarter(memberID, currentYear, currentQuarter);

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
        List<MemberMembershipPeriodVO> periods =
                memberMembershipPeriod.findByMemberIDOrderByStartDateAsc(memberID);

        int joinYear = joinDate.getYear();
        for (int y = joinYear; y <= currentYear; y++) {
            int endQ = (y == currentYear) ? currentQuarter - 1 : 4;
            for (int q = 1; q <= endQ; q++) {
                LocalDate qStart = QuarterUtils.firstDayOfQuarter(y, q);
                LocalDate qEnd = QuarterUtils.lastDayOfQuarter(y, q);

                boolean isActiveInQuarter;
                if (!periods.isEmpty()) {
                    isActiveInQuarter = periods.stream().anyMatch(p ->
                            !p.getStartDate().isAfter(qEnd) && (p.getEndDate() == null || !p.getEndDate().isBefore(qStart)));
                } else {
                    isActiveInQuarter = !joinDate.isAfter(qEnd);
                }

                if (!isActiveInQuarter) {
                    continue; // Skip inactive hiatus quarters
                }

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
    public Page<MemberVO> findMembersActiveInYear(int year, @NonNull Pageable pageable) {
        return getRepo().findAll(MemberSpecifications.wasActiveInYear(year), pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MemberVO> findAllMembersActiveInYear(int year) {
        return getRepo().findAll(MemberSpecifications.wasActiveInYear(year));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MemberMembershipPeriodVO> findMembershipPeriods(@NonNull Integer memberID) {
        return memberMembershipPeriod.findByMemberIDOrderByStartDateAsc(memberID);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Integer, List<MemberMembershipPeriodVO>> findMembershipPeriodsForMembers(@NonNull Collection<Integer> memberIDs) {
        if (memberIDs.isEmpty()) {
            return Collections.emptyMap();
        }
        List<MemberMembershipPeriodVO> all = memberMembershipPeriod.findByMemberIDInOrderByStartDateAsc(memberIDs);
        return all.stream().collect(Collectors.groupingBy(MemberMembershipPeriodVO::getMemberID));
    }

    @Override
    @Transactional(readOnly = true)
    public long countActiveMembersInDateRange(@NonNull LocalDate start, @NonNull LocalDate end) {
        List<Integer> activeIds = memberMembershipPeriod.findActiveMemberIDsInDateRange(start, end);
        if (!activeIds.isEmpty()) {
            return activeIds.size();
        }
        return getRepo().findAll(MemberSpecifications.wasActiveInYear(start.getYear())).stream()
                .filter(m -> {
                    LocalDate join = QuarterUtils.resolveJoinDate(m.getJoinDate());
                    return !join.isAfter(end);
                })
                .count();
    }

    @Override
    @Transactional
    public void activateMemberOnRegistrationFee(@NonNull Integer memberID, @NonNull LocalDate dateReceived) {
        MemberVO existingMember = getRepo().findById(memberID)
                .orElseThrow(() -> new RecordNotFoundException(RECORD_NOT_FOUND.getMessageText()));

        LocalDate joinDate = QuarterUtils.firstDayOfQuarter(dateReceived.getYear(), QuarterUtils.quarterOf(dateReceived));

        if (ReferenceConstants.MEMBER_STATUS.PENDING.equals(existingMember.getStatus())) {
            existingMember.setStatus(ReferenceConstants.MEMBER_STATUS.ACTIVE);
            existingMember.setJoinDate(joinDate);
            update(existingMember);

            memberMembershipPeriod.add(MemberMembershipPeriodVO.builder()
                    .memberID(memberID)
                    .startDate(joinDate)
                    .endDate(null)
                    .build());
        }
    }

    @Override
    @Transactional
    public void updateMemberJoinDateOnRegistrationFee(@NonNull Integer memberID, @NonNull LocalDate dateReceived) {
        MemberVO existingMember = getRepo().findById(memberID)
                .orElseThrow(() -> new RecordNotFoundException(RECORD_NOT_FOUND.getMessageText()));

        LocalDate newJoinDate = QuarterUtils.firstDayOfQuarter(dateReceived.getYear(), QuarterUtils.quarterOf(dateReceived));
        existingMember.setJoinDate(newJoinDate);
        update(existingMember);

        List<MemberMembershipPeriodVO> periods = memberMembershipPeriod.findByMemberIDOrderByStartDateAsc(memberID);
        if (!periods.isEmpty()) {
            MemberMembershipPeriodVO firstPeriod = periods.get(0);
            firstPeriod.setStartDate(newJoinDate);
            memberMembershipPeriod.update(firstPeriod);
        }
    }

    @Override
    @Transactional
    public int deactivateDelinquentMembers(int year) {
        LocalDate yearStart = LocalDate.of(year, 1, 1);
        LocalDate yearEnd = LocalDate.of(year, 12, 31);

        List<MemberVO> activeMembers = getRepo().findAll(MemberSpecifications.hasStatus(ReferenceConstants.MEMBER_STATUS.ACTIVE));
        if (activeMembers.isEmpty()) {
            return 0;
        }

        List<Integer> activeMemberIds = activeMembers.stream().map(MemberVO::getMemberID).toList();
        Map<Integer, List<MemberMembershipPeriodVO>> periodsByMember = findMembershipPeriodsForMembers(activeMemberIds);

        Set<Integer> membersWithPayments = payment.findPaymentSummaries(ReferenceConstants.FEE_TYPE.MEMBERSHIP_FEE, year)
                .stream()
                .map(PaymentSummary::getMemberID)
                .collect(Collectors.toSet());

        int count = 0;
        for (MemberVO memberVO : activeMembers) {
            Integer memberID = memberVO.getMemberID();
            if (membersWithPayments.contains(memberID)) {
                continue;
            }

            List<MemberMembershipPeriodVO> periods = periodsByMember.getOrDefault(memberID, Collections.emptyList());
            boolean activeFullYear = periods.stream().anyMatch(p ->
                    !p.getStartDate().isAfter(yearStart) && (p.getEndDate() == null || p.getEndDate().isAfter(yearEnd)));

            if (activeFullYear) {
                logger.info("Deactivating delinquent member {} for unpaid year {}", memberID, year);
                Optional<MemberMembershipPeriodVO> openPeriod = memberMembershipPeriod
                        .findFirstByMemberIDAndEndDateIsNullOrderByStartDateDesc(memberID);
                if (openPeriod.isPresent()) {
                    MemberMembershipPeriodVO p = openPeriod.get();
                    p.setEndDate(yearEnd);
                    memberMembershipPeriod.update(p);
                }
                memberVO.setStatus(ReferenceConstants.MEMBER_STATUS.INACTIVE);
                update(memberVO);
                count++;
            }
        }
        return count;
    }
}