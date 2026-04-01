import { useEffect, useState } from 'react';
import { systemSettingsApi } from '../api/generated/system-settings/system-settings';
import { SystemSettingsDto } from '../api/generated/types/index';
import { useAuth } from '../hooks/useAuth';
import { useNotification } from '../hooks/useNotification';
import { SystemSettingsContext } from '../hooks/useSystemSettings';

export function SystemSettingsProvider({ children }: { children: React.ReactNode }) {
  const { isAuthenticated } = useAuth();
  const notify = useNotification();
  const [settings, setSettings] = useState<SystemSettingsDto[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  const fetchSettings = async () => {
    setIsLoading(true);
    try {
      const res = await systemSettingsApi.searchSystemSettings({ page: 0, size: 100 });
      if (res.responseData) {
        setSettings(res.responseData);
      }
    } catch (error) {
      notify.error({
        message: 'System Settings Error',
        description: 'Failed to load system settings. Application may not behave as expected.',
      });
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    if (isAuthenticated) {
      fetchSettings();
    } else {
      setSettings([]);
      setIsLoading(false);
    }
  }, [isAuthenticated]);

  const getSettingValue = (name: string, key: string) => {
    const setting = settings.find((s) => s.settingName === name && s.settingKey === key);
    return setting?.settingValue;
  };

  const getNumericSetting = (name: string, key: string) => {
    const val = getSettingValue(name, key);
    if (!val) return 0;
    const num = parseFloat(val);
    return isNaN(num) ? 0 : num;
  };

  return (
    <SystemSettingsContext.Provider
      value={{
        settings,
        getSettingValue,
        getNumericSetting,
        isLoading,
        refreshSettings: fetchSettings,
      }}
    >
      {children}
    </SystemSettingsContext.Provider>
  );
}