import { useMemo } from 'react';
import { useSystemSettings } from './useSystemSettings';

const DEFAULT_INAUGURAL_YEAR = 2026;

export interface UseFiscalYearsResult {
  /**
   * The organization's inaugural baseline year dynamically determined from system settings.
   */
  inauguralYear: number;
  /**
   * Current calendar year on the client.
   */
  currentYear: number;
  /**
   * List of available fiscal years from currentYear down to inauguralYear (descending).
   */
  availableYears: number[];
  /**
   * Formatted option items for Ant Design Select components.
   */
  yearOptions: { value: number; label: string }[];
  /**
   * Loading status of system settings.
   */
  isLoading: boolean;
}

/**
 * Custom hook to dynamically provide fiscal years based on the organization's
 * baseline configurations in System Settings, preventing hardcoded years across pages.
 */
export function useFiscalYears(): UseFiscalYearsResult {
  const { settings, isLoading } = useSystemSettings();
  const currentYear = useMemo(() => new Date().getFullYear(), []);

  const inauguralYear = useMemo(() => {
    const discoveredYears: number[] = [];

    if (settings && settings.length > 0) {
      settings.forEach((s) => {
        // Inspect baseline settings (e.g. BASELINE_REVENUE, YEARLY_BASELINE_2026)
        const isBaseline =
          s.settingName === 'BASELINE_REVENUE' ||
          (s.settingKey && s.settingKey.startsWith('YEARLY_BASELINE_'));

        if (isBaseline) {
          // Extract 4-digit year from settingKey (e.g., YEARLY_BASELINE_2026)
          if (s.settingKey) {
            const match = s.settingKey.match(/(?:YEARLY_BASELINE_)?(\d{4})/);
            if (match && match[1]) {
              const parsed = parseInt(match[1], 10);
              if (!isNaN(parsed) && parsed >= 2000 && parsed <= 2100) {
                discoveredYears.push(parsed);
              }
            }
          }

          // Also inspect effectiveDate (e.g., "2026-01-01") without timezone offset shift
          if (s.effectiveDate) {
            const dateMatch = s.effectiveDate.match(/^(\d{4})/);
            if (dateMatch && dateMatch[1]) {
              const dateYear = parseInt(dateMatch[1], 10);
              if (!isNaN(dateYear) && dateYear >= 2000 && dateYear <= 2100) {
                discoveredYears.push(dateYear);
              }
            }
          }
        }
      });
    }

    if (discoveredYears.length > 0) {
      return Math.min(...discoveredYears);
    }

    return DEFAULT_INAUGURAL_YEAR;
  }, [settings]);

  const availableYears = useMemo(() => {
    const endYear = Math.max(inauguralYear, currentYear);
    const years: number[] = [];
    for (let y = endYear; y >= inauguralYear; y--) {
      years.push(y);
    }
    return years;
  }, [inauguralYear, currentYear]);

  const yearOptions = useMemo(
    () => availableYears.map((y) => ({ value: y, label: String(y) })),
    [availableYears]
  );

  return {
    inauguralYear,
    currentYear,
    availableYears,
    yearOptions,
    isLoading,
  };
}