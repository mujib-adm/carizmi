// enums
export enum Role {
  ROLE_ADMIN = "ADMIN",
  ROLE_MANAGER = "MANAGER",
  ROLE_MEMBER = "MEMBER",
  ROLE_ANONYMOUS = "ANONYMOUS"
}

export enum MessageType {
  ERROR = "ERROR",
  WARNING = "WARNING",
  INFO = "INFO",
  STATUS = "STATUS",
  SUCCESS = "SUCCESS",
  CONFIRMATION = "CONFIRMATION"
}

// Generic API response structure
export interface GlobalResponse<T = any, DataMap = Record<string, string>> {
  statusCode: number;             // e.g. 200, 401, 409
  statusDesc: string;             // e.g. "OK", "Conflict"
  globalMessages: GlobalMsg[];    // list of global messages
  fieldMessages: FieldMsg[];      // list of field-specific messages
  map: DataMap;
  responseData?: T;             // generic payload
  meta?: PaginationMeta;        // pagination info
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

export interface PaginationMeta {
  page: number;
  pageSize: number;
  totalRecords: number;
  totalPages: number;
}

export interface NormalizedResponse {
  statusCode?: number;
  statusDesc?: string;
  globalMessages: string[];              // ["Invalid username or password."]
  fieldMessages: Record<string, string>; // { "email", "Email already exists" }
}

// Login response payload
export interface LoginData {
  token: string;
  role: string;
}

// Profile response payload
export interface ProfileData {
  username: string;
  role: string;
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
export type MemberRequestDto = Omit<Member, "memberID"> & { memberID?: number };

export type MemberSearchParams = {
  firstName?: string;
  lastName?: string;
  phone?: string;
  status?: string;
  // email?: string;
  // city?: string;
  // state?: string;
  // zip?: string;
  // joinDateFrom?: string;
  // joinDateTo?: string;

  // pagination + sorting
  page?: number;       // 0-based page index
  size?: number;       // page size
  sortField?: string;  // optional sort field
  sortOrder?: "asc" | "desc"; // optional sort order
};