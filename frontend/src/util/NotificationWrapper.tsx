import { Modal } from 'antd';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';

export function useNotificationWrapper() {
  const navigate = useNavigate();
  const [modal, setModal] = useState<{
    open: boolean;
    title: string;
    description?: string;
    redirectUrl?: string;
  }>({
    open: false,
    title: '',
    description: '',
    redirectUrl: undefined,
  });

  const showModal = (title: string, description?: string, redirectUrl?: string) => {
    setModal({ open: true, title, description, redirectUrl });
  };

  const closeModal = () => {
    setModal({ ...modal, open: false });
    if (modal.redirectUrl) navigate(modal.redirectUrl);
  };

  const ModalComponent = (
    <Modal
      open={modal.open}
      title={modal.title}
      onOk={closeModal}
      onCancel={closeModal}
      cancelButtonProps={{ style: { display: 'none' } }}
      okButtonProps={{ size: 'large', style: { width: '100px' } }}
    >
      <p>{modal.description}</p>
    </Modal>
  );

  return {
    success: (config: { message: string; description?: string }, redirectUrl?: string) =>
      showModal(config.message, config.description, redirectUrl),
    warning: (config: { message: string; description?: string }, redirectUrl?: string) =>
      showModal(config.message, config.description, redirectUrl),
    ModalComponent,
  };
}
