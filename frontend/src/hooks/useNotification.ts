import { createContext, useContext } from 'react';

export const NotificationContext = createContext<any>(null);

export const useNotification = () => useContext(NotificationContext);