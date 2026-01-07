import { useCallback, useState } from "react";
import { searchSystemSettings } from "../apiclient/systemSettingsApi";
import { GlobalResponse, PaginationMeta, SystemSetting, SystemSettingSearchParams } from "../constants/types";

export function usePaginatedSystemSettings(initialParams: SystemSettingSearchParams = {}) {
    const [settings, setSettings] = useState<SystemSetting[]>([]);
    const [meta, setMeta] = useState<PaginationMeta | null>(null);
    const [loading, setLoading] = useState(false);

    const fetchSettings = useCallback(async (params: SystemSettingSearchParams = {}) => {
        setLoading(true);
        try {
            const mergedParams = { ...initialParams, ...params };
            const resp: GlobalResponse<SystemSetting[]> = await searchSystemSettings(mergedParams);
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