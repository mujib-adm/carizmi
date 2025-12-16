import apiClient from "./ApiClient";
import { GlobalResponse, Member, MemberRequestDto, MemberSearchParams } from "../constants/types";
import { ApiEndpoints } from "../constants/endpoints";

export const addMember = async (payload: MemberRequestDto) => {
  const res = await apiClient.post<GlobalResponse>(ApiEndpoints.MEMBERS.ADD, payload);
  console.log("memberApi.addMember response: ", res);
  return res.data;
};

export const updateMember = async (payload: MemberRequestDto) => {
  const res = await apiClient.put<GlobalResponse>(ApiEndpoints.MEMBERS.UPDATE, payload);
  console.log("memberApi.update response: ", res);
  return res.data;
};

export const deleteMember = async (memberID: number) => {
  const res = await apiClient.delete<GlobalResponse>(ApiEndpoints.MEMBERS.DELETE(memberID));
  console.log("memberApi.delete response: ", res);
  return res.data;
};

export const getMember = async (memberID: number) => {
  const res = await apiClient.get<GlobalResponse<Member>>(ApiEndpoints.MEMBERS.GET(memberID));
  console.log("memberApi.get response: ", res);
  return res.data;
};

export const searchMembers = async (params: MemberSearchParams) => {
  const res = await apiClient.get<GlobalResponse<Member[]>>(ApiEndpoints.MEMBERS.SEARCH, { params });
  console.log("memberApi.searchMembers response: ", res);
  return res.data;
};