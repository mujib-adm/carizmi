import { ApiEndpoints } from '../constants/endpoints';
import { GlobalResponse, ProfileData } from '../constants/types';
import apiClient from './ApiClient';

export const getProfile = async () => {
  const res = await apiClient.get<GlobalResponse<ProfileData>>(ApiEndpoints.AUTH.PROFILE);
  return res.data;
};

export const updatePassword = async (data: any) => {
  const res = await apiClient.post<GlobalResponse>(ApiEndpoints.AUTH.PASSWORD_UPDATE, data);
  return res.data;
};

export const getAllUsers = async () => {
  const res = await apiClient.get(ApiEndpoints.USERS.LIST);
  return res.data;
};

export const updateUserRole = async (userId: number, role: string) => {
  const res = await apiClient.put(ApiEndpoints.USERS.UPDATE_ROLE(userId), { role });
  return res.data;
};

export const updateUserStatus = async (userId: number, active: boolean) => {
  const res = await apiClient.put(ApiEndpoints.USERS.UPDATE_STATUS(userId), { active });
  return res.data;
};