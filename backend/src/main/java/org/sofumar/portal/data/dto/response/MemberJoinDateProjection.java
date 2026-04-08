package org.sofumar.portal.data.dto.response;

import java.time.LocalDate;

/**
 * Lightweight JPA projection for active member join dates, fetches only memberID and joinDate.
 */
public interface MemberJoinDateProjection {
    Integer getMemberID();
    LocalDate getJoinDate();
}