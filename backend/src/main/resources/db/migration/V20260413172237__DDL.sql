--  Create Database Lock Table
--  Initialize Database Lock Table
--  Lock Database
--  Create Database Change Log Table
--  Update Database Script

ALTER TABLE carizmi.expense MODIFY category VARCHAR(3);

ALTER TABLE carizmi.expense MODIFY createdBy VARCHAR(50);

ALTER TABLE carizmi.member MODIFY createdBy VARCHAR(50);

ALTER TABLE carizmi.payment MODIFY createdBy VARCHAR(50);

ALTER TABLE carizmi.`reference` MODIFY createdBy VARCHAR(50);

ALTER TABLE carizmi.systemsettings MODIFY createdBy VARCHAR(50);

ALTER TABLE carizmi.users MODIFY createdBy VARCHAR(50);

ALTER TABLE carizmi.payment MODIFY feeType VARCHAR(3);

ALTER TABLE carizmi.expense MODIFY lastModifiedBy VARCHAR(50);

ALTER TABLE carizmi.member MODIFY lastModifiedBy VARCHAR(50);

ALTER TABLE carizmi.payment MODIFY lastModifiedBy VARCHAR(50);

ALTER TABLE carizmi.`reference` MODIFY lastModifiedBy VARCHAR(50);

ALTER TABLE carizmi.systemsettings MODIFY lastModifiedBy VARCHAR(50);

ALTER TABLE carizmi.users MODIFY lastModifiedBy VARCHAR(50);

ALTER TABLE carizmi.users MODIFY lockoutTime datetime(6);

ALTER TABLE carizmi.payment MODIFY methodOfPayment VARCHAR(3);

ALTER TABLE carizmi.member MODIFY state VARCHAR(3);

ALTER TABLE carizmi.member MODIFY status VARCHAR(3);

--  Release Database Lock
