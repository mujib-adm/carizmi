import React, { createContext, useContext } from 'react';
import { useNotificationWrapper } from '../util/NotificationWrapper';

const NotificationContext = createContext<any>(null);

export function NotificationProvider({ children }: { children: React.ReactNode }) {
  const notify = useNotificationWrapper();

  return (
    <NotificationContext.Provider value={notify}>
      {children}
      {notify.ModalComponent}
    </NotificationContext.Provider>
  );
}

export const useNotification = () => useContext(NotificationContext);
