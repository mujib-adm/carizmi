import React, { createContext, useEffect, useState } from 'react';
import GradientSpinner from './GradientSpinnerContext';

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
  } else {
    // Optional: keep this quiet in production; useful during development
    // console.warn("No loading setter registered yet. Call registerLoadingSetter from LoadingProvider.");
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

// CSS styles (inject once)
const styles = `
.overlay {
  position: fixed;
  top: 0; left: 0;
  width: 100vw; height: 100vh;
  background-color: rgba(0,0,0,0.3);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 9999;
  opacity: 0;
  pointer-events: none; /* block clicks only when visible */
  transition: opacity 0.3s ease;
}
.overlay.fade-in {
  opacity: 1;
  pointer-events: auto;
}
.overlay.fade-out {
  opacity: 0;
  pointer-events: none;
}
.spinner {
  display: inline-block;
  width: 64px;
  height: 64px;
}
.spinner:after {
  content: " ";
  display: block;
  width: 46px;
  height: 46px;
  margin: 1px;
  border-radius: 50%;
  border: 10px solid #1E5631;
  border-color: #1E5631 #AEDF88 #1E5631 #AEDF88;
  animation: dual-ring 1.2s linear infinite;
}
@keyframes dual-ring {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}
`;
document.head.insertAdjacentHTML('beforeend', `<style>${styles}</style>`);
