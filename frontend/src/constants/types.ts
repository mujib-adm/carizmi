// enums
export enum MessageType {
  ERROR = 'ERROR',
  WARNING = 'WARNING',
  INFO = 'INFO',
  STATUS = 'STATUS',
  SUCCESS = 'SUCCESS',
  CONFIRMATION = 'CONFIRMATION',
}

export interface Pagination {
  page?: number; // 0-based page index
  size?: number; // page size
  sortField?: string; // optional sort field
  sortOrder?: 'asc' | 'desc'; // optional sort order
}

export interface PaginationMeta {
  page: number;
  pageSize: number;
  totalRecords: number;
  totalPages: number;
}

// Generic API response structure
export interface GlobalResponse<T = any, DataMap = Record<string, string>> {
  statusCode: number; // e.g. 200, 401, 409
  statusDesc: string; // e.g. "OK", "Conflict"
  globalMessages: GlobalMsg[]; // list of global messages
  fieldMessages: FieldMsg[]; // list of field-specific messages
  map: DataMap;
  responseData?: T; // generic payload
  meta?: PaginationMeta; // pagination info
}

export interface GlobalMsg {
  type: MessageType;
  message: string;
}

export interface FieldMsg {
  type: MessageType;
  field: string;
  message: string;
}

// Profile response payload
export interface ProfileData {
  username: string;
  role: string;
  firstName: string;
  lastName: string;
  email: string;
}

// User types
export interface User {
  userID: number;
  firstName: string;
  lastName: string;
  email: string;
  username: string;
  role: string;
  active: boolean;
}

// Forms
export interface LoginForm {
  username: string;
  password: string;
}

export interface RegisterForm {
  firstName: string;
  lastName: string;
  email: string;
  username: string;
  password: string;
}

// Member types
export type Member = {
  memberID: number;
  firstName: string;
  lastName: string;
  phone: string;
  email?: string;
  status: string;
  joinDate?: string;
  address1?: string;
  address2?: string;
  city?: string;
  state: string;
  zip?: string;
};
export type MemberRequestDto = Omit<Member, 'memberID'> & { memberID?: number };

export interface MemberSearchRequest extends Pagination {
  firstName?: string;
  lastName?: string;
  phone?: string;
  status?: string;
}

export type MemberLookupResponse = {
  memberID: number;
  firstName: string;
  lastName: string;
  phone: string;
};

export type MemberSummary = {
  totalPaid: number;
  outstanding: number;
  overdue: number;
};

// Payment types
export type Payment = {
  paymentID: number;
  memberID: number;
  memberFullName: string;
  feeType: string;
  amount: number;
  dateReceived: string;
  methodOfPayment: string;
  year?: number;
  quarter?: number;
};

export type PaymentRequestDto = Omit<Payment, 'paymentID'> & { paymentID?: number };

export interface PaymentSearchRequest extends Pagination {
  memberID?: number;
  feeType?: string;
  year?: number;
  quarter?: number;
  dateFrom?: string;
  dateTo?: string;
  dateRange?: string;
}

// Expense types
export type Expense = {
  expenseID: number;
  dateOfExpense: string;
  category: string;
  description: string;
  amount: number;
};

export interface ExpenseSearchRequest extends Pagination {
  category?: string;
  dateFrom?: string;
  dateTo?: string;
}

// System Setting types
export type SystemSetting = {
  systemSettingsID: number;
  settingName: string;
  settingKey: string;
  settingValue: string;
};

export interface SystemSettingSearchRequest extends Pagination {
  settingName?: string;
}

// Reference types
export type Reference = {
  referenceID: number;
  referenceName: string;
  referenceCode: string;
  referenceDisplay: string;
  active: boolean;
};

export type ReferenceRequestDto = Omit<Reference, 'referenceID'> & { referenceID?: number };

export interface ReferenceSearchRequest extends Pagination {
  referenceName?: string;
}

export interface ReferenceData {
  code: string;
  display: string;
}

// Dashboard
export interface DashboardMetrics {
  totalMembers: number;
  totalRevenue: number;
  duesThisQuarter: number;
  overdueTotal: number;
  quarterlyFeeAmt: number;
  quarterlyCollections: QuarterlyCollection[];
}

export interface QuarterlyCollection {
  quarterLabel: string;
  collectedAmount: number;
  percentage: number;
  status: 'PAST' | 'CURRENT' | 'FUTURE';
}

export interface RecentTransactions {
  paymentID: number;
  memberID?: number;
  paymentDate: string;
  memberName: string;
  feeType: string;
  amount: number;
}