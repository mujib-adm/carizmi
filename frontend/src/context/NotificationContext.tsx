// NotificationContext.tsx
import React, { createContext, useContext } from "react";
import { useNotificationWrapper } from "../util/NotificationWrapper";

const NotificationContext = createContext<any>(null);

export const NotificationProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const notify = useNotificationWrapper();

  return (
    <NotificationContext.Provider value={notify}>
      {children}
      {notify.ModalComponent}
    </NotificationContext.Provider>
  );
};

export const useNotification = () => useContext(NotificationContext);
