import { ApiEndpoints } from "../constants/endpoints";
import { GlobalResponse, Reference, ReferenceRequestDto, ReferenceSearchParams } from "../constants/types";
import apiClient from "./ApiClient";

export const searchReferences = async (params: ReferenceSearchParams) => {
    const res = await apiClient.post<GlobalResponse<Reference[]>>(ApiEndpoints.REFERENCE.SEARCH, params);
    return res.data;
};

export const getReferencesByName = async (referenceName: string) => {
    const res = await apiClient.get<GlobalResponse<Reference[]>>(ApiEndpoints.REFERENCE.GET_BY_NAME(referenceName));
    return res.data;
};