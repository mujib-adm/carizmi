/**
 * Reference Code Constants
 * These should match the backend ReferenceConstants.java
 */
export const ReferenceConstants = {
  MEMBER_STATUS: {
    NAME: 'memberStatus',
    ACTIVE: '01',
    PENDING: '02',
    INACTIVE: '03',
  },
  FEE_TYPE: {
    NAME: 'feeType',
    MEMBERSHIP_FEE: '01',
    REGISTRATION_FEE: '02',
  },
  PAYMENT_METHOD: {
    NAME: 'paymentMethod',
    CASH: '01',
    ZELLE: '02',
    CHECK: '03',
    DIRECT_DEPOSIT: '04',
  },
  EXPENSE_CATEGORY: {
    NAME: 'expenseCategory',
    COMMUNITY_PICNIC: '01',
    OFFICE_SUPPLIES: '02',
    TRANSPORTATION: '03',
    EVENT: '04',
    OTHER: '05',
  },
};

export const STARTUP_REFERENCES = [
  ReferenceConstants.MEMBER_STATUS.NAME,
  ReferenceConstants.FEE_TYPE.NAME,
  ReferenceConstants.PAYMENT_METHOD.NAME,
  ReferenceConstants.EXPENSE_CATEGORY.NAME,
];
