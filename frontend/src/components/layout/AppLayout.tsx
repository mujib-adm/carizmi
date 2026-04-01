import { Outlet } from 'react-router-dom';
import Sidebar from './Sidebar';

export default function AppLayout() {
  return (
    <div className="dashboard-layout">
      <Sidebar />
      <main className="content fade-in">
        <Outlet />
      </main>
    </div>
  );
}