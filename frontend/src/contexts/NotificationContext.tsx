import React from 'react';
import { useNotificationWrapper } from '../hooks/useNotificationWrapper';
import { NotificationContext } from '../hooks/useNotification';

export function NotificationProvider({ children }: { children: React.ReactNode }) {
  const notify = useNotificationWrapper();

  return (
    <NotificationContext.Provider value={notify}>
      {children}
      {notify.ModalComponent}
    </NotificationContext.Provider>
  );
}