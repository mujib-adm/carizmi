import { createContext, useContext } from 'react';
import { SystemSettingsDto } from '../api/generated/types/index';

export type SystemSettingsContextType = {
  settings: SystemSettingsDto[];
  getSettingValue: (name: string, key: string) => string | undefined;
  getNumericSetting: (name: string, key: string) => number;
  isLoading: boolean;
  refreshSettings: () => Promise<void>;
};

export const SystemSettingsContext = createContext<SystemSettingsContextType>({
  settings: [],
  getSettingValue: () => undefined,
  getNumericSetting: () => 0,
  isLoading: true,
  refreshSettings: async () => {},
});

export function useSystemSettings() {
  return useContext(SystemSettingsContext);
}