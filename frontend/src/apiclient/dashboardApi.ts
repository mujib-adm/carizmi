import { ApiEndpoints } from "../constants/endpoints";
import { DashboardMetrics, GlobalResponse, RecentTransactions } from "../constants/types";
import apiClient from "./ApiClient";

export const getDashboardMetrics = async () => {
    const res = await apiClient.get<GlobalResponse<DashboardMetrics>>(ApiEndpoints.DASHBOARD.METRICS);
    return res.data;
};

export const getLatestPayments = async () => {
    const res = await apiClient.get<GlobalResponse<RecentTransactions[]>>(ApiEndpoints.PAYMENTS.LATEST);
    return res.data;
};