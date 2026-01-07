import {
    DashboardOutlined,
    DatabaseOutlined,
    DollarOutlined,
    LogoutOutlined,
    SettingOutlined,
    ShoppingCartOutlined,
    TeamOutlined,
    UserOutlined
} from '@ant-design/icons';
import type { MenuProps } from 'antd';
import { Menu } from 'antd';
import { useLocation, useNavigate } from 'react-router-dom';
import './Sidebar.css';

export default function Sidebar() {
    const role = localStorage.getItem("role") || "USER";
    const navigate = useNavigate();
    const location = useLocation();

    const items: MenuProps['items'] = [
        { key: '/dashboard', icon: <DashboardOutlined />, label: 'Dashboard' },
        { key: '/payments', icon: <DollarOutlined />, label: 'Payments' },
        { key: '/expenses', icon: <ShoppingCartOutlined />, label: 'Expenses' },
        { key: '/members', icon: <TeamOutlined />, label: 'Members' },
//         { key: '/reporting', icon: <FileSearchOutlined />, label: 'Reporting' },
        { key: '/references', icon: <DatabaseOutlined />, label: 'Reference' },
        { key: '/settings', icon: <SettingOutlined />, label: 'System Settings' },
        ...(role === "ADMIN" ? [{ key: '/users', icon: <UserOutlined />, label: 'User' }] : []),
        { key: '/logout', icon: <LogoutOutlined />, label: 'Logout' },
    ];

    const onClick: MenuProps['onClick'] = (e) => {
        navigate(e.key);
    };

    return (
        <aside className="sidebar-container">
            <Menu
                onClick={onClick}
                selectedKeys={[location.pathname === '/' ? '/dashboard' : location.pathname]}
                mode="inline"
                items={items}
                className="modern-sidebar-menu"
            />
        </aside>
    );
}