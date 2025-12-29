
export const ApiEndpoints = {
  MEMBERS: {
    ADD: "/members/add",
    UPDATE: "/members/update",
    SEARCH: "/members/search",
    LOOKUP: "/members/lookup",
    GET: (memberID: number) => `/members/get/${memberID}`,
    DELETE: (memberID: number) => `/members/delete/${memberID}`,
  },
  PAYMENTS: {
    ADD: "/payments/add",
    UPDATE: "/payments/update",
    SEARCH: "/payments/search",
    GET: (paymentID: number) => `/payments/get/${paymentID}`,
    DELETE: (paymentID: number) => `/payments/delete/${paymentID}`,
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
};