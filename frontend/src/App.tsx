import { Image, Layout } from "antd";
import { BrowserRouter } from "react-router-dom";
import Router from "./pages/Router";
import { AuthProvider } from "./context/AuthContext";
import Logo from "./images/logo.png";
import { NotificationProvider } from "./context/NotificationContext";
import { ReferenceProvider } from "./context/ReferenceContext";

const { Header, Content, Footer } = Layout;
const year = new Date().getFullYear();

export default function App() {
  return (
    <AuthProvider>
      <ReferenceProvider>
        <Layout className="app-wrapper">
          <BrowserRouter>
            <NotificationProvider>
              <Header className="header">
                <Image src={Logo} alt="Logo" preview={false} height="68px" width="68px" className="header-logo" />
                <h2 className="header-title"> Sof'umar Portal </h2>
              </Header>

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