import apiClient from './ApiClient';
import type { AxiosRequestConfig, AxiosResponse } from 'axios';

/**
 * Custom Axios mutator for Orval-generated API functions.
 *
 * This thin wrapper delegates to `apiClient` instance,
 * which already has interceptors for auth refresh, loading state,
 * and error handling configured.
 *
 * @see https://orval.dev/reference/configuration/output#mutator
 */
export const apiMutator = async <T>(config: AxiosRequestConfig): Promise<T> => {
  const response: AxiosResponse<T> = await apiClient(config);
  return response.data;
};

export default apiMutator;