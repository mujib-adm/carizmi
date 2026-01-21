import { createContext, useContext, useEffect, useState } from "react";
import { searchSystemSettings } from "../apiclient/systemSettingsApi";
import { SystemSetting } from "../constants/types";
import { useAuth } from "./AuthContext";
import { useNotification } from "./NotificationContext";

type SystemSettingsContextType = {
    settings: SystemSetting[];
    getSettingValue: (type: string, key: string) => string | undefined;
    getNumericSetting: (type: string, key: string) => number;
    isLoading: boolean;
    refreshSettings: () => Promise<void>;
};

const SystemSettingsContext = createContext<SystemSettingsContextType>({
    settings: [],
    getSettingValue: () => undefined,
    getNumericSetting: () => 0,
    isLoading: true,
    refreshSettings: async () => {},
});

export function SystemSettingsProvider({ children }: { children: React.ReactNode }) {
    const { token } = useAuth();
    const notify = useNotification();
    const [settings, setSettings] = useState<SystemSetting[]>([]);
    const [isLoading, setIsLoading] = useState(true);

    const fetchSettings = async () => {
        setIsLoading(true);
        try {
            const res = await searchSystemSettings({ page: 0, size: 100 }); // Fetch all reasonable settings
            if (res.responseData) {
                setSettings(res.responseData);
            }
        } catch (error) {
            notify.error({ 
                message: "System Settings Error", 
                description: "Failed to load system settings. Application may not behave as expected." 
            });
        } finally {
            setIsLoading(false);
        }
    };

    useEffect(() => {
        if (token) {
            fetchSettings();
        } else {
            setSettings([]);
            setIsLoading(false);
        }
    }, [token]);

    const getSettingValue = (type: string, key: string) => {
        const setting = settings.find(s => s.settingType === type && s.settingKey === key);
        return setting?.settingValue;
    };

    const getNumericSetting = (type: string, key: string) => {
        const val = getSettingValue(type, key);
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
                refreshSettings: fetchSettings 
            }}
        >
            {children}
        </SystemSettingsContext.Provider>
    );
}

export function useSystemSettings() {
    return useContext(SystemSettingsContext);
}