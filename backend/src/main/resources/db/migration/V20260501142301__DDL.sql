--  Create Database Lock Table
--  Initialize Database Lock Table
--  Lock Database
--  Create Database Change Log Table
--  Update Database Script

ALTER TABLE `expense`         ADD COLUMN `version` BIGINT NOT NULL DEFAULT 0;
ALTER TABLE `member`          ADD COLUMN `version` BIGINT NOT NULL DEFAULT 0;
ALTER TABLE `payment`         ADD COLUMN `version` BIGINT NOT NULL DEFAULT 0;
ALTER TABLE `reference`       ADD COLUMN `version` BIGINT NOT NULL DEFAULT 0;
ALTER TABLE `systemsettings`  ADD COLUMN `version` BIGINT NOT NULL DEFAULT 0;
ALTER TABLE `users`           ADD COLUMN `version` BIGINT NOT NULL DEFAULT 0;

DROP TABLE IF EXISTS carizmi.dashboard_metrics_snapshot;
CREATE TABLE carizmi.dashboard_metrics_snapshot (
    id                    INT            DEFAULT 1 NOT NULL,
    dues_this_quarter     DECIMAL(38, 2) DEFAULT 0 NOT NULL,
    last_projected_at     datetime(6) DEFAULT NOW(6) NOT NULL,
    overdue_total         DECIMAL(38, 2) DEFAULT 0 NOT NULL,
    quarterly_collections JSON NULL,
    quarterly_fee_amt     DECIMAL(38, 2) DEFAULT 0 NOT NULL,
    total_active_members  BIGINT         DEFAULT 0 NOT NULL,
    total_revenue         DECIMAL(38, 2) DEFAULT 0 NOT NULL,
    CONSTRAINT dashboard_metrics_snapshotPK PRIMARY KEY (id),
    CONSTRAINT chk_singleton CHECK (id = 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--  Release Database Lock
