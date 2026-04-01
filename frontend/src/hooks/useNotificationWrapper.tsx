import { Modal } from 'antd';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { CheckCircleOutlined, WarningOutlined } from '@ant-design/icons';

type NotificationType = 'success' | 'warning';

export function useNotificationWrapper() {
  const navigate = useNavigate();
  const [modal, setModal] = useState<{
    open: boolean;
    title: string;
    description?: string;
    redirectUrl?: string;
    type: NotificationType;
  }>({
    open: false,
    title: '',
    description: '',
    redirectUrl: undefined,
    type: 'success',
  });

  const showModal = (
    type: NotificationType,
    title: string,
    description?: string,
    redirectUrl?: string,
  ) => {
    setModal({ open: true, title, description, redirectUrl, type });
  };

  const closeModal = () => {
    setModal({ ...modal, open: false });
    if (modal.redirectUrl) navigate(modal.redirectUrl);
  };

  const iconMap: Record<NotificationType, React.ReactNode> = {
    success: <CheckCircleOutlined className="notify-icon notify-icon--success" />,
    warning: <WarningOutlined className="notify-icon notify-icon--warning" />,
  };

  const ModalComponent = (
    <Modal
      open={modal.open}
      title={modal.title}
      onOk={closeModal}
      onCancel={closeModal}
      cancelButtonProps={{ style: { display: 'none' } }}
      okButtonProps={{ size: 'large', style: { width: '100px' } }}
      centered
      className={`modern-modal notify-modal notify-modal--${modal.type}`}
    >
      <div className="notify-body">
        {iconMap[modal.type]}
        <p className="notify-description">{modal.description}</p>
      </div>
    </Modal>
  );

  return {
    success: (config: { message: string; description?: string }, redirectUrl?: string) =>
      showModal('success', config.message, config.description, redirectUrl),
    warning: (config: { message: string; description?: string }, redirectUrl?: string) =>
      showModal('warning', config.message, config.description, redirectUrl),
    ModalComponent,
  };
}
