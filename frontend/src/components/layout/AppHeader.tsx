import { UserOutlined } from '@ant-design/icons';
import { Avatar, Image, Layout, Space, Tooltip } from 'antd';
import { useCallback, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import { useTheme } from '../../hooks/useTheme';
import { getBranding } from '../../config/branding';
import autoThemeIcon from '../../assets/icons/auto-theme.svg';
import darkThemeIcon from '../../assets/icons/dark-theme.svg';
import lightThemeIcon from '../../assets/icons/light-theme.svg';
import Logo from '../../assets/images/logo.png';
import styles from '../../styles/components/AppHeader.module.css';

const { Header } = Layout;

/** Maps each preference to its icon, tooltip, and aria-label */
const THEME_UI = {
  system: {
    icon: <img src={autoThemeIcon} alt="Auto theme" className={styles.themeIconImg} />,
    tooltip: 'Auto (System)',
    ariaLabel: 'Current theme: Auto. Click to switch to Dark.',
  },
  dark: {
    icon: <img src={darkThemeIcon} alt="Dark theme" className={styles.themeIconImg} />,
    tooltip: '',
    ariaLabel: 'Current theme: Dark. Click to switch to Light.',
  },
  light: {
    icon: <img src={lightThemeIcon} alt="Light theme" className={styles.themeIconImg} />,
    tooltip: '',
    ariaLabel: 'Current theme: Light. Click to switch to Auto.',
  },
} as const;

export default function AppHeader() {
  const { isAuthenticated, firstName } = useAuth();
  const { preference, cycleTheme } = useTheme();
  const [flipping, setFlipping] = useState(false);
  const branding = getBranding();

  const handleToggle = useCallback(() => {
    setFlipping(true);
    // Switch theme at the midpoint of the flip (when icon is hidden)
    setTimeout(() => cycleTheme(), 250);
    // Remove animation class after it completes so it can re-trigger
    setTimeout(() => setFlipping(false), 500);
  }, [cycleTheme]);

  const toggleClasses = [
    styles.themeToggle,
    flipping ? styles.themeToggleFlip : '',
  ].filter(Boolean).join(' ');

  const ui = THEME_UI[preference];

  return (
    <Header className={styles.glassHeader}>
      <div className={styles.logoContainer}>
        <Image src={Logo} alt={branding.logoAlt} preview={false} className={styles.logo} />
        <div className={styles.titleStack}>
          <h2 className={styles.titleMain}>{branding.headerTitle}</h2>
          <span className={styles.titleSub}>{branding.headerSubtitle}</span>
        </div>
      </div>

      <div className={styles.headerActions}>
        <Space size={16}>
          <Tooltip title={ui.tooltip} placement="bottom">
            <div
              role="button"
              tabIndex={0}
              aria-label={ui.ariaLabel}
              onClick={handleToggle}
              onKeyDown={(e) => e.key === 'Enter' && handleToggle()}
              className={toggleClasses}
            >
              {ui.icon}
            </div>
          </Tooltip>
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