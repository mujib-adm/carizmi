import { Image, Layout } from "antd";
import { BrowserRouter } from "react-router-dom";
import Router from "./pages/Router";
import { AuthProvider } from "./context/AuthContext";
import { NotificationProvider } from "./context/NotificationContext";
import { ReferenceProvider } from "./context/ReferenceContext";
import AppHeader from "./pages/AppHeader";

const { Content, Footer } = Layout;
const year = new Date().getFullYear();

export default function App() {
  return (
    <AuthProvider>
      <ReferenceProvider>
        <Layout className="app-wrapper">
          <BrowserRouter>
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
      </ReferenceProvider>
    </AuthProvider>
  );
}