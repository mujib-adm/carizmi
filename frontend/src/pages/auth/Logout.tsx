import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { useNotification } from '../../context/NotificationContext';

export default function Logout() {
  const navigate = useNavigate();
  const { logout } = useAuth();
  const notify = useNotification();

  useEffect(() => {
    try {
      logout(); // calls API to clear server-side cookies, then clears local state
    } catch (err) {
      notify.warning({
        message: 'Logout Warning',
        description: 'Server-side logout may have failed, but you have been logged out locally.',
      });
    }

    navigate('/login'); // redirect
  }, [logout, navigate, notify]);

  return <div>Logging out...</div>;
}