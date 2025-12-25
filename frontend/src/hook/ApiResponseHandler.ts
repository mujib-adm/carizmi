import { AxiosError } from "axios";
import { useCallback, useState } from "react";
import { FieldValues, Path, UseFormSetError } from "react-hook-form";
import { GlobalMsg, GlobalResponse } from "../constants/types";

export function useApiMessages<T extends FieldValues>(
  setError?: UseFormSetError<T>,
  onFieldError?: (field: string, message: string) => void
) {
  const [globalMessages, setGlobalMessages] = useState<GlobalMsg[] | null>(null);

  /**
   * Handle a successful API response.
   * Extracts globalMessages for display.
   */
  const handleResponse = useCallback(<R>(response: GlobalResponse<R>) => {
    if (response) {
      setGlobalMessages(
        response.globalMessages?.map((msg) => ({
          type: msg.type,
          message: msg.message
        })) ?? null
      );
    }
  }, []);

  /**
   * Handle an API error response.
   * Maps fieldMessages into react-hook-form errors if setError is provided,
   * or calls onFieldError callback if provided.
   * Also extracts globalMessages for display.
   */
  const handleError = useCallback((error: AxiosError<GlobalResponse<any>>) => {
    if (error?.response?.data) {
      const data = error.response.data;

      // Map backend fieldMessages to RHF errors or Generic Callback
      if (data.fieldMessages) {
        data.fieldMessages.forEach((fm) => {
          if (fm.field && fm.message) {
            if (setError) {
              setError(fm.field as Path<T>, { type: "server", message: fm.message });
            } else if (onFieldError) {
              onFieldError(fm.field, fm.message);
            }
          }
        });
      }

      // Extract globalMessages to display on MessageBanner
      setGlobalMessages(
        data.globalMessages?.map((msg) => ({
          type: msg.type,
          message: msg.message,
        })) ?? null
      );
    }
  }, [setError, onFieldError]);

  /** Reset all messages */
  const resetMessages = useCallback(() => setGlobalMessages(null), []);

  return { globalMessages, handleResponse, handleError, resetMessages };
}