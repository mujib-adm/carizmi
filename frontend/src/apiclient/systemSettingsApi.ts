import { ApiEndpoints } from '../constants/endpoints';
import { GlobalResponse, SystemSetting, SystemSettingSearchRequest } from '../constants/types';
import apiClient from './ApiClient';

export const getSettingsByKey = async (key: string) => {
  const res = await apiClient.get<GlobalResponse<SystemSetting[]>>(ApiEndpoints.SETTINGS.GETBYKEY(key));
  return res.data;
};

export const updateSystemSetting = async (data: any) => {
  const res = await apiClient.put<GlobalResponse>(ApiEndpoints.SETTINGS.UPDATE, data);
  return res.data;
};

export const searchSystemSettings = async (request: SystemSettingSearchRequest) => {
  const res = await apiClient.post<GlobalResponse<SystemSetting[]>>(ApiEndpoints.SETTINGS.SEARCH, request);
  return res.data;
};