import { ApiEndpoints } from "../constants/endpoints";
import { GlobalResponse, SystemSetting, SystemSettingSearchParams } from "../constants/types";
import apiClient from "./ApiClient";

export const getSettingsByKey = async (key: string) => {
    const res = await apiClient.get<GlobalResponse<SystemSetting[]>>(ApiEndpoints.SETTINGS.GETBYKEY(key));
    return res.data;
};

export const updateSystemSetting = async (data: any) => {
    return await apiClient.put("/system-settings/update", data);
};

export const searchSystemSettings = async (params: SystemSettingSearchParams) => {
  const res = await apiClient.post<GlobalResponse<SystemSetting[]>>(ApiEndpoints.SETTINGS.SEARCH, params);
  return res.data;
};