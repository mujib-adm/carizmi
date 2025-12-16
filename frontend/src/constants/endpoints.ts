
export const ApiEndpoints = {
  MEMBERS: {
    ADD: "/members/add",
    UPDATE: "/members/update",
    SEARCH: "/members/search",
    GET: (memberID: number) => `/members/get/${memberID}`,
    DELETE: (memberID: number) => `/members/delete/${memberID}`,
  },
};