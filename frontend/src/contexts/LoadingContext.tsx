import React, { createContext, useEffect, useState } from 'react';
import GradientSpinner from '../components/GradientSpinner';
import '../styles/components/LoadingOverlay.css';

type LoadingContextType = {
  isLoading: boolean;
  setLoading: (loading: boolean) => void;
};

export const LoadingContext = createContext<LoadingContextType>({
  isLoading: false,
  setLoading: () => {},
});

/**
 * Registration API for external code (interceptors, utilities) to control loading.
 * We export a stable registration function and a setter function that calls the registered setter.
 * This avoids exporting a mutable singleton object that changes shape on every render,
 * which helps HMR / Fast Refresh remain stable.
 */
let registeredSetter: ((v: boolean) => void) | null = null;

export const registerLoadingSetter = (fn: ((v: boolean) => void) | null) => {
  registeredSetter = fn;
};

export const setGlobalLoading = (v: boolean) => {
  if (registeredSetter) {
    registeredSetter(v);
  }
};

export function LoadingProvider({ children }: { children: React.ReactNode }) {
  const [isLoading, setIsLoading] = useState(false);

  // Register the setter once so external code can call setGlobalLoading(...)
  useEffect(() => {
    registerLoadingSetter(setIsLoading);
    return () => {
      registerLoadingSetter(null);
    };
  }, [setIsLoading]);

  return (
    <LoadingContext.Provider value={{ isLoading, setLoading: setIsLoading }}>
      {children}
      <LoadingOverlay visible={isLoading} />
    </LoadingContext.Provider>
  );
}

function LoadingOverlay({ visible }: { visible: boolean }) {
  return (
    <div className={`overlay ${visible ? 'fade-in' : 'fade-out'}`}>
      {visible && <GradientSpinner />}
    </div>
  );
}
