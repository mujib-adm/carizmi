import { ApiEndpoints } from "../constants/endpoints";
import { GlobalResponse, Payment, PaymentRequestDto, PaymentSearchParams } from "../constants/types";
import apiClient from "./ApiClient";

export const addPayment = async (data: PaymentRequestDto) => {
  const res = await apiClient.post<GlobalResponse>(ApiEndpoints.PAYMENTS.ADD, data);
  return res.data;
};

export const updatePayment = async (data: PaymentRequestDto) => {
  const res = await apiClient.put<GlobalResponse>(ApiEndpoints.PAYMENTS.UPDATE, data);
  return res.data;
};

export const deletePayment = async (paymentID: number) => {
  const res = await apiClient.delete<GlobalResponse>(ApiEndpoints.PAYMENTS.DELETE(paymentID));
  return res.data;
};

export const searchPayments = async (params: PaymentSearchParams) => {
  const res = await apiClient.get<GlobalResponse<Payment[]>>(ApiEndpoints.PAYMENTS.SEARCH, { params });
  return res.data;
};