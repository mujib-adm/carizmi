import { UserOutlined } from '@ant-design/icons';
import { Avatar, Image, Layout, Space, Tooltip } from 'antd';
import { Link } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import Logo from '../../assets/images/logo.png';
import styles from '../../styles/components/AppHeader.module.css';

const { Header } = Layout;

export default function AppHeader() {
  const { isAuthenticated, firstName } = useAuth();

  return (
    <Header className={styles.glassHeader}>
      <div className={styles.logoContainer}>
        <Image src={Logo} alt="Sof'umar Logo" preview={false} className={styles.logo} />
        <div className={styles.titleStack}>
          <h2 className={styles.titleMain}>SOF'UMAR</h2>
          <span className={styles.titleSub}>COMMUNITY OF MINNESOTA</span>
        </div>
      </div>

      <div className={styles.headerActions}>
        <Space size={24}>
          {isAuthenticated && (
            <Tooltip title={`Welcome, ${firstName}`} placement="bottom">
              <Link to="/profile">
                <Avatar
                  size={44}
                  icon={<UserOutlined />}
                  style={{
                    backgroundColor: '#40916C',
                    cursor: 'pointer',
                    boxShadow: '0 4px 12px rgba(64, 145, 108, 0.25)',
                    border: '2px solid rgba(255, 255, 255, 0.8)',
                  }}
                />
              </Link>
            </Tooltip>
          )}
        </Space>
      </div>
    </Header>
  );
}