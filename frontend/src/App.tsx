import { Layout } from 'antd';
import { BrowserRouter } from 'react-router-dom';
import { AuthProvider } from './contexts/AuthContext';
import { NotificationProvider } from './contexts/NotificationContext';
import { ReferenceProvider } from './contexts/ReferenceContext';
import { SystemSettingsProvider } from './contexts/SystemSettingsContext';
import ErrorBoundary from './components/ErrorBoundary';
import AppHeader from './components/layout/AppHeader';
import Router from './Router';

const { Content, Footer } = Layout;
const year = new Date().getFullYear();

export default function App() {
  return (
    <ErrorBoundary>
      <AuthProvider>
        <ReferenceProvider>
          <SystemSettingsProvider>
            <Layout className="app-wrapper">
              <BrowserRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
                <NotificationProvider>
                  <AppHeader />
                  <Layout className="main-layout">
                    <Content className="app-main">
                      <Router />
                    </Content>
                    <Footer className="copyright">
                      <p>© {year} Sof'umar Community of Minnesota.</p>
                    </Footer>
                  </Layout>
                </NotificationProvider>
              </BrowserRouter>
            </Layout>
          </SystemSettingsProvider>
        </ReferenceProvider>
      </AuthProvider>
    </ErrorBoundary>
  );
}