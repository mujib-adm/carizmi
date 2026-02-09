import { ApiEndpoints } from "../constants/endpoints";
import { GlobalResponse, Payment, PaymentRequestDto, PaymentSearchRequest } from "../constants/types";
import apiClient from "./ApiClient";

export const addPayment = async (data: PaymentRequestDto) => {
  const res = await apiClient.post<GlobalResponse<number>>(ApiEndpoints.PAYMENTS.ADD, data);
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

export const getPayment = async (paymentID: number) => {
  const res = await apiClient.get<GlobalResponse<Payment>>(ApiEndpoints.PAYMENTS.GET(paymentID));
  return res.data;
};

export const searchPayments = async (request: PaymentSearchRequest) => {
  const res = await apiClient.post<GlobalResponse<Payment[]>>(ApiEndpoints.PAYMENTS.SEARCH, request);
  return res.data;
};