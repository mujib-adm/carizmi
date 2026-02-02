import { ApiEndpoints } from "../constants/endpoints";
import { Expense, ExpenseSearchParams, GlobalResponse } from "../constants/types";
import apiClient from "./ApiClient";

export const addExpense = async (data: Omit<Expense, 'expenseID'>) => {
  const res = await apiClient.post<GlobalResponse<number>>(ApiEndpoints.EXPENSES.ADD, data);
  return res.data;
};

export const updateExpense = async (data: Expense) => {
  const res = await apiClient.put<GlobalResponse>(ApiEndpoints.EXPENSES.UPDATE, data);
  return res.data;
};

export const deleteExpense = async (expenseID: number) => {
  const res = await apiClient.delete<GlobalResponse>(ApiEndpoints.EXPENSES.DELETE(expenseID));
  return res.data;
};

export const getExpense = async (expenseID: number) => {
  const res = await apiClient.get<GlobalResponse<Expense>>(ApiEndpoints.EXPENSES.GET(expenseID));
  return res.data;
};

export const searchExpenses = async (params: ExpenseSearchParams) => {
  const res = await apiClient.post<GlobalResponse<Expense[]>>(ApiEndpoints.EXPENSES.SEARCH, params);
  return res.data;
};