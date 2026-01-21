import { AxiosError } from "axios";
import { useCallback, useState } from "react";
import { FieldValues, Path, UseFormSetError } from "react-hook-form";
import { GlobalMsg, GlobalResponse, MessageType } from "../constants/types";

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
    // 1. Backend returned a standard GlobalResponse error
    if (error?.response?.data && (error.response.data.globalMessages || error.response.data.fieldMessages)) {
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
      return;
    }

    // 2. Fallback: Network Error, Timeout, or Backend crashed (500 with HTML)
    let fallbackMsg = "An unexpected error occurred. Please try again.";

    if (error.code === "ERR_NETWORK") {
      fallbackMsg = "Network Error. Please check your internet connection.";
    } else if (error.code === "ECONNABORTED") {
      fallbackMsg = "Request timed out. Please try again later.";
    } else if (error.response?.status === 500) {
      fallbackMsg = "Internal Server Error. Please contact support.";
    } else if (error.response?.status === 403) {
      fallbackMsg = "You don't have permission to perform this action.";
    } else if (error.message) {
      fallbackMsg = error.message;
    }

    setGlobalMessages([{ type: MessageType.ERROR, message: fallbackMsg }]);
  }, [setError, onFieldError]);

  /** Reset all messages */
  const resetMessages = useCallback(() => setGlobalMessages(null), []);

  return { globalMessages, handleResponse, handleError, resetMessages };
}