import { useCallback, useState } from 'react';
import { searchSystemSettings } from '../apiclient/systemSettingsApi';
import {
  GlobalResponse,
  PaginationMeta,
  SystemSetting,
  SystemSettingSearchRequest,
} from '../constants/types';

export function usePaginatedSystemSettings(initialRequest: SystemSettingSearchRequest = {}) {
  const [settings, setSettings] = useState<SystemSetting[]>([]);
  const [meta, setMeta] = useState<PaginationMeta | null>(null);
  const [loading, setLoading] = useState(false);

  const fetchSettings = useCallback(async (request: SystemSettingSearchRequest = {}) => {
    setLoading(true);
    try {
      const mergedRequest = { ...initialRequest, ...request };
      const resp: GlobalResponse<SystemSetting[]> = await searchSystemSettings(mergedRequest);
      setSettings(resp.responseData ?? []);
      setMeta(resp.meta ?? null);
    } catch (error) {
      throw error;
    } finally {
      setLoading(false);
    }
  }, []);

  return { settings, meta, loading, fetchSettings, setSettings };
}