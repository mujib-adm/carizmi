package io.carizmi.domain.membership.service;

import io.carizmi.domain.membership.model.MemberMembershipPeriodVO;
import io.carizmi.framework.bl.BusinessLogic;
import org.springframework.lang.NonNull;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MemberMembershipPeriod extends BusinessLogic<MemberMembershipPeriodVO> {

    List<MemberMembershipPeriodVO> findByMemberIDOrderByStartDateAsc(@NonNull Integer memberID);

    List<MemberMembershipPeriodVO> findByMemberIDInOrderByStartDateAsc(@NonNull Collection<Integer> memberIDs);

    Optional<MemberMembershipPeriodVO> findFirstByMemberIDAndEndDateIsNullOrderByStartDateDesc(@NonNull Integer memberID);

    boolean isMemberActiveInDateRange(@NonNull Integer memberID, @NonNull LocalDate rangeStart, @NonNull LocalDate rangeEnd);

    List<Integer> findActiveMemberIDsInDateRange(@NonNull LocalDate rangeStart, @NonNull LocalDate rangeEnd);

    boolean isMemberActiveInQuarter(@NonNull Integer memberID, int year, int quarter);
}