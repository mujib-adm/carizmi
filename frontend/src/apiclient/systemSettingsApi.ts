import { ApiEndpoints } from "../constants/endpoints";
import { GlobalResponse, SystemSetting } from "../constants/types";
import apiClient from "./ApiClient";

export const getSettingsByKey = async (key: string) => {
    const res = await apiClient.get<GlobalResponse<SystemSetting[]>>(ApiEndpoints.SETTINGS.GETBYKEY(key));
    return res.data;
};

export const updateSystemSetting = async (data: any) => {
    return await apiClient.put("/system-settings/update", data);
};

export const searchSystemSettings = async (params: any) => {
  const res = await apiClient.get<GlobalResponse<SystemSetting[]>>(ApiEndpoints.SETTINGS.SEARCH, { params });
  return res.data;
};