import { useCallback, useMemo, useState } from 'react';
import { systemSettingsApi } from '../../../api/generated/system-settings/system-settings';
import {
  PaginationMeta,
  SystemSettingsDto,
  SystemSettingsSearchRequestDto,
} from '../../../api/generated/types';

export function usePaginatedSystemSettings(initialRequest: SystemSettingsSearchRequestDto = {}) {
  const [settings, setSettings] = useState<SystemSettingsDto[]>([]);
  const [meta, setMeta] = useState<PaginationMeta | null>(null);
  const [loading, setLoading] = useState(false);

  // Stabilize initialRequest
  const memoInitialRequest = useMemo(() => initialRequest, [JSON.stringify(initialRequest)]);

  const fetchSettings = useCallback(
    async (request: SystemSettingsSearchRequestDto = {}) => {
      setLoading(true);
      try {
        const mergedRequest = { ...memoInitialRequest, ...request };
        const resp = await systemSettingsApi.searchSystemSettings(mergedRequest);
        setSettings(resp.responseData ?? []);
        setMeta(resp.meta ?? null);
      } catch (error) {
        throw error;
      } finally {
        setLoading(false);
      }
    },
    [memoInitialRequest]
  );

  return { settings, meta, loading, fetchSettings, setSettings };
}