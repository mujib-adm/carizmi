--  Create Database Lock Table
--  Initialize Database Lock Table
--  Lock Database
--  Create Database Change Log Table
--  Update Database Script

ALTER TABLE carizmi.expense MODIFY category VARCHAR(3) NOT NULL;

ALTER TABLE carizmi.expense MODIFY createdBy VARCHAR(50) NOT NULL;

ALTER TABLE carizmi.member MODIFY createdBy VARCHAR(50) NOT NULL;

ALTER TABLE carizmi.payment MODIFY createdBy VARCHAR(50) NOT NULL;

ALTER TABLE carizmi.`reference` MODIFY createdBy VARCHAR(50) NOT NULL;

ALTER TABLE carizmi.systemsettings MODIFY createdBy VARCHAR(50) NOT NULL;

ALTER TABLE carizmi.users MODIFY createdBy VARCHAR(50) NOT NULL;

ALTER TABLE carizmi.payment MODIFY feeType VARCHAR(3) NOT NULL;

ALTER TABLE carizmi.member MODIFY joinDate date NOT NULL;

ALTER TABLE carizmi.payment MODIFY methodOfPayment VARCHAR(3) NOT NULL;

ALTER TABLE carizmi.member MODIFY state VARCHAR(3) NOT NULL;

ALTER TABLE carizmi.member MODIFY status VARCHAR(3) NOT NULL;

--  Release Database Lock
