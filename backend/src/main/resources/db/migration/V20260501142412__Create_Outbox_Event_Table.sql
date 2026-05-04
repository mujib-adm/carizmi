-- =====================================================================
-- Creates the outbox_event table for the Transactional Outbox Pattern.

-- aggregate_type:  VO class name (e.g., UserVO, MemberVO)
-- aggregate_id:    Primary key of the domain entity
-- event_type:      CREATED, UPDATED, or DELETED
-- payload:         Serialized ValueObject at the time of the event
-- processed_at:    NULL = pending, set when successfully relayed
-- =====================================================================

DROP TABLE IF EXISTS carizmi.outbox_event;
CREATE TABLE carizmi.outbox_event (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    aggregate_type   VARCHAR(100)  NOT NULL COMMENT 'VO class name (e.g., UserVO, MemberVO)',
    aggregate_id     INT           NOT NULL COMMENT 'Primary key of the domain entity',
    event_type       VARCHAR(50)   NOT NULL COMMENT 'CREATED, UPDATED, or DELETED',
    payload          JSON          NOT NULL COMMENT 'Serialized ValueObject at the time of the event',
    created_at       DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    processed_at     DATETIME(6)   NULL     COMMENT 'NULL = pending, set when successfully relayed',
    INDEX idx_outbox_unprocessed (processed_at, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
