-- 1. Modify joinDate column on member table to allow NULL (for pending members prior to activation)
ALTER TABLE carizmi.member MODIFY joinDate DATE NULL;

-- 2. Create CQRS read model table for frozen annual financial report snapshots
CREATE TABLE IF NOT EXISTS carizmi.annual_report_snapshot (
    fiscal_year            INT            NOT NULL,
    report_data            JSON           NOT NULL,
    beginning_cash_balance DECIMAL(38, 2) NOT NULL,
    total_receipts         DECIMAL(38, 2) NOT NULL,
    total_disbursements    DECIMAL(38, 2) NOT NULL,
    ending_cash_balance    DECIMAL(38, 2) NOT NULL,
    active_members_count   BIGINT         NOT NULL,
    closed_at              DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    closed_by              VARCHAR(50)    NOT NULL DEFAULT 'SYSTEM_SCHEDULER',
    version                BIGINT         NOT NULL DEFAULT 0,
    createdDateTime        DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    createdBy              VARCHAR(50)    NOT NULL DEFAULT 'SYSTEM_SCHEDULER',
    lastModifiedDateTime   DATETIME(6)    NULL ON UPDATE CURRENT_TIMESTAMP(6),
    lastModifiedBy         VARCHAR(50)    NULL,
    CONSTRAINT annual_report_snapshotPK PRIMARY KEY (fiscal_year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Create the membership period table to track discrete active spans
CREATE TABLE IF NOT EXISTS carizmi.member_membership_period (
    periodID             INT         NOT NULL AUTO_INCREMENT,
    memberID             INT         NOT NULL,
    startDate            DATE        NOT NULL,
    endDate              DATE        NULL,
    version              BIGINT      NOT NULL DEFAULT 0,
    createdDateTime      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    createdBy            VARCHAR(50) NOT NULL DEFAULT 'SYSTEM_MIGRATION',
    lastModifiedDateTime DATETIME(6) NULL ON UPDATE CURRENT_TIMESTAMP(6),
    lastModifiedBy       VARCHAR(50) NULL,
    PRIMARY KEY (periodID),
    KEY idx_mmp_member (memberID),
    KEY idx_mmp_dates (startDate, endDate),
    CONSTRAINT fk_mmp_member FOREIGN KEY (memberID) REFERENCES carizmi.member (memberID) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Idempotent Data Backfill for Existing Production Records
-- Backfill Active Members: startDate = joinDate, endDate = NULL
INSERT INTO carizmi.member_membership_period (memberID, startDate, endDate, version, createdDateTime, createdBy)
SELECT 
    m.memberID,
    COALESCE(m.joinDate, CURDATE()),
    NULL,
    0,
    NOW(6),
    'DATA_BACKFILL'
FROM carizmi.member m
WHERE m.status = '01'
  AND NOT EXISTS (SELECT 1 FROM carizmi.member_membership_period p WHERE p.memberID = m.memberID);

-- Backfill Inactive Members: startDate = joinDate, endDate = joinDate (closed span)
INSERT INTO carizmi.member_membership_period (memberID, startDate, endDate, version, createdDateTime, createdBy)
SELECT 
    m.memberID,
    COALESCE(m.joinDate, CURDATE()),
    COALESCE(m.joinDate, CURDATE()),
    0,
    NOW(6),
    'DATA_BACKFILL'
FROM carizmi.member m
WHERE m.status = '03'
  AND NOT EXISTS (SELECT 1 FROM carizmi.member_membership_period p WHERE p.memberID = m.memberID);

-- Note: Pending members (status = '02') receive no active period until approved/activated via Registration Fee.
