package io.carizmi.shared.data.dto;

import java.time.LocalDate;

/**
 * Lightweight JPA projection for active member join dates, fetches only memberID and joinDate.
 */
public interface MemberJoinDateProjection {
    Integer getMemberID();
    LocalDate getJoinDate();
}