
export const ApiEndpoints = {
  MEMBERS: {
    ADD: "/members/add",
    UPDATE: "/members/update",
    SEARCH: "/members/search",
    LOOKUP: "/members/lookup",
    GET: (memberID: number) => `/members/get/${memberID}`,
    DELETE: (memberID: number) => `/members/delete/${memberID}`,
    SUMMARY: (memberID: number) => `/members/${memberID}/summary`,
  },
  PAYMENTS: {
    ADD: "/payments/add",
    UPDATE: "/payments/update",
    SEARCH: "/payments/search",
    GET: (paymentID: number) => `/payments/get/${paymentID}`,
    DELETE: (paymentID: number) => `/payments/delete/${paymentID}`,
    LATEST: "/payments/latest",
  },
  EXPENSES: {
    ADD: "/expenses/add",
    UPDATE: "/expenses/update",
    SEARCH: "/expenses/search",
    GET: (expenseID: number) => `/expenses/get/${expenseID}`,
    DELETE: (expenseID: number) => `/expenses/delete/${expenseID}`,
  },
  DASHBOARD: {
    METRICS: "/dashboard/metrics",
  },
  SETTINGS: {
    ADD: "/system-settings/add",
    UPDATE: "/system-settings/update",
    SEARCH: "/system-settings/search",
    GET: (settingID: number) => `/system-settings/get/${settingID}`,
    GETBYKEY: (key: string) => `/system-settings/by-key/${key}`,
    DELETE: (settingID: number) => `/system-settings/delete/${settingID}`,
  },
  REFERENCE: {
    SEARCH: "/reference/search",
    GET_BY_NAME: (referenceName: string) => `/reference/list/${referenceName}`,
  },
  AUTH: {
    LOGIN: "/auth/login",
    REGISTER: "/auth/register",
    PROFILE: "/auth/profile",
    PASSWORD_UPDATE: "/auth/password-update",
  },
};