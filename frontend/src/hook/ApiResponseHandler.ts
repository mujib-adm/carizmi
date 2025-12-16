import { useState } from "react";
import { AxiosError } from "axios";
import { FieldValues, Path, UseFormSetError } from "react-hook-form";
import { GlobalResponse, GlobalMsg } from "../constants/types";

export function useApiMessages<T extends FieldValues>(setError?: UseFormSetError<T>) {
  const [globalMessages, setGlobalMessages] = useState<GlobalMsg[] | null>(null);

  /**
   * Handle a successful API response.
   * Extracts globalMessages for display.
   */
  const handleResponse = <R>(response: GlobalResponse<R>) => {
    if (response) {
      setGlobalMessages(
        response.globalMessages?.map((msg) => ({
          type: msg.type,
          message: msg.message
        })) ?? null
      );
    }
  };

  /**
   * Handle an API error response.
   * Maps fieldMessages into react-hook-form errors if setError is provided,
   * and extracts globalMessages for display.
   */
  const handleError = (error: AxiosError<GlobalResponse<any>>) => {
    if (error?.response?.data) {
      const data = error.response.data;

      // Map backend fieldMessages to RHF errors
      if (setError && data.fieldMessages) {
        data.fieldMessages.forEach((fm) => {
          if (fm.field && fm.message) {
            setError(fm.field as Path<T>, { type: "server", message: fm.message, });
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
  };

  /** Reset all messages */
  const resetMessages = () => setGlobalMessages(null);

  return { globalMessages, handleResponse, handleError, resetMessages };
}