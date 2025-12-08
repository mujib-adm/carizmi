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
export interface GlobalResponse<DataMap = Record<string, string>> {
  statusCode: number;             // e.g. 200, 401, 409
  statusDesc: string;             // e.g. "OK", "Conflict"
  globalMessages: GlobalMsg[];    // list of global messages
  fieldMessages: FieldMsg[];      // list of field-specific messages
  map: DataMap;
}

export interface GlobalMsg {
  type: MessageType;
  message: string;
}

export interface FieldMsg {
  type: MessageType;
  message: string;
  fieldName: string;
}

export interface NormalizedResponse {
  statusCode?: number;
  statusDesc?: string;
  fieldMessages: Record<string, string>; // { "email", "Email already exists" }
  globalMessages: string[];            // ["Invalid username or password."]
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