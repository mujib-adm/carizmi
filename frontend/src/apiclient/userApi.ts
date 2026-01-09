import { ApiEndpoints } from "../constants/endpoints";
import { GlobalResponse, ProfileData } from "../constants/types";
import apiClient from "./ApiClient";

export const getProfile = async () => {
    const res = await apiClient.get<GlobalResponse<ProfileData>>(ApiEndpoints.AUTH.PROFILE);
    return res.data;
};

export const updatePassword = async (data: any) => {
    const res = await apiClient.post<GlobalResponse>(ApiEndpoints.AUTH.PASSWORD_UPDATE, data);
    return res.data;
};