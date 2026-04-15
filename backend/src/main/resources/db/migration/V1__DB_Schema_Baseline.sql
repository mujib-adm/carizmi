/*
 * Initial Schema Baseline (V1)
 * Engines: InnoDB
 * Charset: utf8mb4
 * Collation: utf8mb4_unicode_ci
 */

/*
 * =========================================================================================
 * 🚨 DO NOT MODIFY THIS FILE 🚨
 * =========================================================================================
 * 
 * This file represents the initial baseline schema (V1).
 * It has already been executed in production/staging environments.
 * 
 * Modifying this file will cause checksum validation errors in Flyway and break deployments.
 * 
 * TO MAKE CHANGES:
 * 1. Modify the Java Entity/VO classes.
 * 2. Run the MigrationDdlGenerator to create a NEW migration file (e.g., V20260101120000__DDL.sql).
 * 
 * =========================================================================================
 */

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- -----------------------------------------------------
-- Table `expense`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `expense`;
CREATE TABLE `expense` (
  `expenseID` int NOT NULL AUTO_INCREMENT,
  `category` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `amount` decimal(14,2) NOT NULL,
  `dateOfExpense` date NOT NULL,
  `description` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `createdBy` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `createdDateTime` datetime(6) NOT NULL,
  `lastModifiedBy` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `lastModifiedDateTime` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`expenseID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- -----------------------------------------------------
-- Table `member`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `member`;
CREATE TABLE `member` (
  `memberID` int NOT NULL AUTO_INCREMENT,
  `firstName` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `lastName` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `phone` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `email` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `joinDate` date DEFAULT NULL,
  `address1` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `address2` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `city` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `state` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `zip` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `createdBy` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `createdDateTime` datetime(6) NOT NULL,
  `lastModifiedBy` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `lastModifiedDateTime` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`memberID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- -----------------------------------------------------
-- Table `payment`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `payment`;
CREATE TABLE `payment` (
  `paymentID` int NOT NULL AUTO_INCREMENT,
  `memberID` int NOT NULL,
  `feeType` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `amount` decimal(14,2) NOT NULL,
  `dateReceived` date NOT NULL,
  `year` int DEFAULT NULL,
  `quarter` int DEFAULT NULL,
  `methodOfPayment` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `createdBy` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `createdDateTime` datetime(6) NOT NULL,
  `lastModifiedBy` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `lastModifiedDateTime` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`paymentID`),
  KEY `fk_payment_member` (`memberID`),
  CONSTRAINT `fk_payment_member` FOREIGN KEY (`memberID`) REFERENCES `member` (`memberID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- -----------------------------------------------------
-- Table `reference`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `reference`;
CREATE TABLE `reference` (
  `referenceID` int NOT NULL AUTO_INCREMENT,
  `referenceName` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `referenceCode` varchar(3) COLLATE utf8mb4_unicode_ci NOT NULL,
  `referenceDisplay` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `active` bit(1) NOT NULL,
  `createdBy` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `createdDateTime` datetime(6) NOT NULL,
  `lastModifiedBy` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `lastModifiedDateTime` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`referenceID`),
  UNIQUE KEY `UKs69ywevqvxq2bubvp7bppllal` (`referenceName`,`referenceCode`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- -----------------------------------------------------
-- Table `systemsettings`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `systemsettings`;
CREATE TABLE `systemsettings` (
  `systemSettingsID` int NOT NULL AUTO_INCREMENT,
  `settingName` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `settingKey` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `settingValue` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `active` bit(1) DEFAULT NULL,
  `effectiveDate` date DEFAULT NULL,
  `createdBy` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `createdDateTime` datetime(6) NOT NULL,
  `lastModifiedBy` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `lastModifiedDateTime` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`systemSettingsID`),
  UNIQUE KEY `UKqm1i13dqj9onirju0c2fgij5i` (`settingKey`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- -----------------------------------------------------
-- Table `users`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
  `userID` int NOT NULL AUTO_INCREMENT,
  `firstName` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `lastName` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `email` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `username` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `password` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `role` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `passwordUpdatedAt` datetime(6) DEFAULT NULL,
  `active` bit(1) DEFAULT NULL,
  `failedLoginAttempts` int DEFAULT 0,
  `lockoutTime` datetime DEFAULT NULL,
  `createdBy` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `createdDateTime` datetime(6) NOT NULL,
  `lastModifiedBy` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `lastModifiedDateTime` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`userID`),
  UNIQUE KEY `UK6dotkott2kjsp8vw4d0m25fb7` (`email`),
  UNIQUE KEY `UKr43af9ap4edm43mmtq01oddj6` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;
