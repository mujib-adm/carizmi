import { MoonOutlined, SunOutlined, UserOutlined } from '@ant-design/icons';
import { Avatar, Image, Layout, Space, Tooltip } from 'antd';
import { useCallback, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import { useTheme } from '../../hooks/useTheme';
import Logo from '../../assets/images/logo.png';
import styles from '../../styles/components/AppHeader.module.css';

const { Header } = Layout;

export default function AppHeader() {
  const { isAuthenticated, firstName } = useAuth();
  const { theme, toggleTheme } = useTheme();
  const [flipping, setFlipping] = useState(false);

  const handleToggle = useCallback(() => {
    setFlipping(true);
    // Switch theme at the midpoint of the flip (when icon is hidden)
    setTimeout(() => toggleTheme(), 250);
    // Remove animation class after it completes so it can re-trigger
    setTimeout(() => setFlipping(false), 500);
  }, [toggleTheme]);

  const toggleClasses = [
    styles.themeToggle,
    flipping ? styles.themeToggleFlip : '',
  ].filter(Boolean).join(' ');

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
        <Space size={16}>
          <div
            role="button"
            tabIndex={0}
            aria-label={theme === 'dark' ? 'Switch to light mode' : 'Switch to dark mode'}
            onClick={handleToggle}
            onKeyDown={(e) => e.key === 'Enter' && handleToggle()}
            className={toggleClasses}
          >
            {theme === 'dark' ? <SunOutlined /> : <MoonOutlined />}
          </div>
          {isAuthenticated && (
            <Tooltip title={`Welcome, ${firstName}`} placement="bottom">
              <Link to="/profile">
                <Avatar
                  size={44}
                  icon={<UserOutlined />}
                  className={`brand-avatar ${styles.avatar}`}
                />
              </Link>
            </Tooltip>
          )}
        </Space>
      </div>
    </Header>
  );
}