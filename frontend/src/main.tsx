import { ConfigProvider, theme } from 'antd';
import { LoadingProvider } from './context/LoadingContext.tsx';
import ReactDOM from 'react-dom/client';
import './styles/global.css';
import './styles/design-system.css';
import './styles/ant-overrides.css';
import App from './App';

const sofumarTheme = {
  token: {
    colorPrimary: '#1E5631', // Deep forest green
    colorSuccess: '#5C9013', // Olive green
    colorWarning: '#AEDF88', // Light green
    colorError: '#c62828', // Alert red
    colorBgBase: '#f0fdf4', // Soft green background
    fontFamily: 'Poppins, sans-serif',
    borderRadius: 8,
  },
};

const rootElement = document.getElementById('root');
if (!rootElement) {
  throw new Error('Root element with id "root" not found');
}

ReactDOM.createRoot(rootElement).render(
  <ConfigProvider theme={sofumarTheme}>
    <LoadingProvider>
      <App />
    </LoadingProvider>
  </ConfigProvider>
);
