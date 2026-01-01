import {
    DashboardOutlined,
    UserOutlined,
    DollarOutlined,
    SolutionOutlined,
    FileSearchOutlined,
    SettingOutlined,
    TeamOutlined,
    LogoutOutlined,
    DatabaseOutlined
} from '@ant-design/icons';
import { Menu } from 'antd';
import type { MenuProps } from 'antd';
import { useNavigate, useLocation } from 'react-router-dom';
import './Sidebar.css';

export default function Sidebar() {
    const role = localStorage.getItem("role") || "USER";
    const navigate = useNavigate();
    const location = useLocation();

    const items: MenuProps['items'] = [
        { key: '/dashboard', icon: <DashboardOutlined />, label: 'Dashboard' },
        { key: '/payments', icon: <DollarOutlined />, label: 'Payments' },
        { key: '/members', icon: <UserOutlined />, label: 'Members' },
//         { key: '/expenses', icon: <SolutionOutlined />, label: 'Expenses' },
//         { key: '/reporting', icon: <FileSearchOutlined />, label: 'Reporting' },
        { key: '/references', icon: <DatabaseOutlined />, label: 'Reference' },
        { key: '/settings', icon: <SettingOutlined />, label: 'System Settings' },
        ...(role === "ADMIN" ? [{ key: '/users', icon: <TeamOutlined />, label: 'Users' }] : []),
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