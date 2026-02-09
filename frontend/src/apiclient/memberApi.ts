import { ApiEndpoints } from "../constants/endpoints";
import { GlobalResponse, Member, MemberLookupResponse, MemberRequestDto, MemberSearchRequest, MemberSummary } from "../constants/types";
import apiClient from "./ApiClient";

export const addMember = async (data: MemberRequestDto) => {
  const res = await apiClient.post<GlobalResponse<number>>(ApiEndpoints.MEMBERS.ADD, data);
  return res.data;
};

export const updateMember = async (data: MemberRequestDto) => {
  const res = await apiClient.put<GlobalResponse>(ApiEndpoints.MEMBERS.UPDATE, data);
  return res.data;
};

export const deleteMember = async (memberID: number) => {
  const res = await apiClient.delete<GlobalResponse>(ApiEndpoints.MEMBERS.DELETE(memberID));
  return res.data;
};

export const getMember = async (memberID: number) => {
  const res = await apiClient.get<GlobalResponse<Member>>(ApiEndpoints.MEMBERS.GET(memberID));
  return res.data;
};

export const searchMembers = async (request: MemberSearchRequest) => {
  const res = await apiClient.post<GlobalResponse<Member[]>>(ApiEndpoints.MEMBERS.SEARCH, request);
  return res.data;
};

export const lookupMembers = async (query: string) => {
  const res = await apiClient.get<GlobalResponse<MemberLookupResponse[]>>(ApiEndpoints.MEMBERS.LOOKUP, { params: { query } });
  return res.data;
};

export const getMemberSummary = async (memberID: number) => {
  const res = await apiClient.get<GlobalResponse<MemberSummary>>(ApiEndpoints.MEMBERS.SUMMARY(memberID));
  return res.data;
};