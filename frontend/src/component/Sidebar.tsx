import { NavLink } from 'react-router-dom';
import './Sidebar.css';

export default function Sidebar() {
    const role = localStorage.getItem("role") || "USER";

    return (
        <nav className="sidebar">
            <ul>
                <li><NavLink to="/dashboard" className={({ isActive }) => isActive ? "active" : ""}>Dashboard</NavLink></li>
                <li><NavLink to="/members" className={({ isActive }) => isActive ? "active" : ""}>Members</NavLink></li>
                <li><NavLink to="/payments" className={({ isActive }) => isActive ? "active" : ""}>Payments</NavLink></li>
                <li><NavLink to="/expenses" className={({ isActive }) => isActive ? "active" : ""}>Expenses</NavLink></li>
                <li><NavLink to="/reporting" className={({ isActive }) => isActive ? "active" : ""}>Reporting</NavLink></li>
                <li><NavLink to="/references" className={({ isActive }) => isActive ? "active" : ""}>Reference</NavLink></li>
                <li><NavLink to="/settings" className={({ isActive }) => isActive ? "active" : ""}>System Settings</NavLink></li>
                {role === "ADMIN" && <li><NavLink to="/users" className={({ isActive }) => isActive ? "active" : ""}>Users</NavLink></li>}
                <li><NavLink to="/logout">Logout</NavLink></li>
            </ul>
        </nav>
    );
}