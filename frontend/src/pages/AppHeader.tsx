import { UserOutlined } from "@ant-design/icons";
import { Avatar, Image, Layout, Space, Tooltip } from "antd";
import Logo from "../images/logo.png";
import { useAuth } from "../context/AuthContext";
import "../themes/modern-ui.css";

const { Header } = Layout;

export default function AppHeader() {
  const { token, firstName } = useAuth();

  return (
    <Header className="glass-header">
      <div className="portal-logo-container">
        <Image
          src={Logo}
          alt="Sof'umar Logo"
          preview={false}
          className="portal-logo-modern"
        />
        <div className="portal-title-stack">
          <h2 className="portal-title-main">SOF'UMAR</h2>
          <span className="portal-title-sub">COMMUNITY OF MINNESOTA</span>
        </div>
      </div>

      <div className="header-actions">
        <Space size={24}>
          {token && (
            <Tooltip title={`Welcome, ${firstName}`} placement="bottom">
              <Avatar
                size={44}
                icon={<UserOutlined />}
                style={{
                  backgroundColor: '#40916C',
                  cursor: 'pointer',
                  boxShadow: '0 4px 12px rgba(64, 145, 108, 0.25)',
                  border: '2px solid rgba(255, 255, 255, 0.8)'
                }}
              />
            </Tooltip>
          )}
        </Space>
      </div>
    </Header>
  );
}