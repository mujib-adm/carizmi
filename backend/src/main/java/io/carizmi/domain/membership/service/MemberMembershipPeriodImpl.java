package io.carizmi.domain.membership.service;

import io.carizmi.domain.membership.model.MemberMembershipPeriodVO;
import io.carizmi.domain.membership.repository.MemberMembershipPeriodRepository;
import io.carizmi.domain.membership.validation.MemberMembershipPeriodValidator;
import io.carizmi.shared.util.QuarterUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Service
public final class MemberMembershipPeriodImpl extends MemberMembershipPeriodAbstractBL implements MemberMembershipPeriod {

    private static final Logger logger = LoggerFactory.getLogger(MemberMembershipPeriodImpl.class);

    private final MemberMembershipPeriodValidator validator;

    @Autowired
    public MemberMembershipPeriodImpl(MemberMembershipPeriodRepository repo, MemberMembershipPeriodValidator validator) {
        super(repo);
        this.validator = validator;
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }

    @Override
    protected void performDomainValidation(MemberMembershipPeriodVO vo, boolean isUpdate) {
        if (isUpdate) {
            validator.validateForUpdate(vo);
        } else {
            validator.validate(vo);
        }
    }

    @Override
    protected void performDeleteValidation(MemberMembershipPeriodVO vo) {
        validator.validateForDelete(vo);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MemberMembershipPeriodVO> findByMemberIDOrderByStartDateAsc(@NonNull Integer memberID) {
        return getRepo().findByMemberIDOrderByStartDateAsc(memberID);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MemberMembershipPeriodVO> findByMemberIDInOrderByStartDateAsc(@NonNull Collection<Integer> memberIDs) {
        return getRepo().findByMemberIDInOrderByStartDateAsc(memberIDs);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MemberMembershipPeriodVO> findFirstByMemberIDAndEndDateIsNullOrderByStartDateDesc(@NonNull Integer memberID) {
        return getRepo().findFirstByMemberIDAndEndDateIsNullOrderByStartDateDesc(memberID);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isMemberActiveInDateRange(@NonNull Integer memberID, @NonNull LocalDate rangeStart, @NonNull LocalDate rangeEnd) {
        return getRepo().isMemberActiveInDateRange(memberID, rangeStart, rangeEnd);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Integer> findActiveMemberIDsInDateRange(@NonNull LocalDate rangeStart, @NonNull LocalDate rangeEnd) {
        return getRepo().findActiveMemberIDsInDateRange(rangeStart, rangeEnd);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isMemberActiveInQuarter(@NonNull Integer memberID, int year, int quarter) {
        LocalDate qStart = QuarterUtils.firstDayOfQuarter(year, quarter);
        LocalDate qEnd = QuarterUtils.lastDayOfQuarter(year, quarter);
        return isMemberActiveInDateRange(memberID, qStart, qEnd);
    }
}