import { ConfigProvider, theme as antTheme } from 'antd';
import { LoadingProvider } from './contexts/LoadingContext.tsx';
import { ThemeProvider } from './contexts/ThemeContext.tsx';
import { useTheme } from './hooks/useTheme.ts';
import ReactDOM from 'react-dom/client';
import './styles/global.css';
import './styles/design-system.css';
import './styles/ant-overrides.css';
import App from './App';

const lightTheme = {
  token: {
    colorPrimary: '#1E5631',
    colorSuccess: '#5C9013',
    colorWarning: '#AEDF88',
    colorError: '#c62828',
    colorBgBase: '#f0fdf4',
    fontFamily: 'Poppins, sans-serif',
    borderRadius: 8,
  },
};

const darkTheme = {
  algorithm: antTheme.darkAlgorithm,
  token: {
    colorPrimary: '#52B788',
    colorSuccess: '#74C69D',
    colorWarning: '#ffa726',
    colorError: '#ef5350',
    colorBgBase: '#0f1419',
    fontFamily: 'Poppins, sans-serif',
    borderRadius: 8,
  },
};

function ThemedApp() {
  const { theme } = useTheme();
  return (
    <ConfigProvider theme={theme === 'dark' ? darkTheme : lightTheme}>
      <LoadingProvider>
        <App />
      </LoadingProvider>
    </ConfigProvider>
  );
}

const rootElement = document.getElementById('root');
if (!rootElement) {
  throw new Error('Root element with id "root" not found');
}

ReactDOM.createRoot(rootElement).render(
  <ThemeProvider>
    <ThemedApp />
  </ThemeProvider>
);
