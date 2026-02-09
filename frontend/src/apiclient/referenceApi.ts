import { ApiEndpoints } from "../constants/endpoints";
import { GlobalResponse, Reference, ReferenceRequestDto, ReferenceSearchRequest } from "../constants/types";
import apiClient from "./ApiClient";

export const searchReferences = async (request: ReferenceSearchRequest) => {
    const res = await apiClient.post<GlobalResponse<Reference[]>>(ApiEndpoints.REFERENCE.SEARCH, request);
    return res.data;
};

export const getReferencesByName = async (referenceName: string) => {
    const res = await apiClient.get<GlobalResponse<Reference[]>>(ApiEndpoints.REFERENCE.GET_BY_NAME(referenceName));
    return res.data;
};