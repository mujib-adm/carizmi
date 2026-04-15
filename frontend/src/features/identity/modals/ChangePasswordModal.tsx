import { Col, Divider, Form, Modal, Row, Typography } from 'antd';
import { PasswordUpdateRequestDto } from '../../../api/generated/types';
import { useApiMessages } from '../../../hooks/useApiMessages';
import { AntdFormItem } from '../../../components/AntdFormItem';
import { MessageBanner } from '../../../components/MessageBanner';

const { Text } = Typography;

interface ChangePasswordModalProps {
  open: boolean;
  onCancel: () => void;
  onSubmit: (values: PasswordUpdateRequestDto) => Promise<void>;
}

export default function ChangePasswordModal({
  open,
  onCancel,
  onSubmit,
}: ChangePasswordModalProps) {
  const [form] = Form.useForm();
  const { globalMessages, handleError, resetMessages } = useApiMessages(undefined, (field, msg) => {
    // Backend VO field is "password", but modal form field is "newPassword"
    const formField = field === 'password' ? 'newPassword' : field;
    form.setFields([{ name: formField, errors: [msg] }]);
  });

  const handleOk = async () => {
    try {
      resetMessages();
      const values = await form.validateFields();
      await onSubmit(values as PasswordUpdateRequestDto);
    } catch (e: any) {
      if (e.errorFields) {
        // Ant Design form validation error, do nothing
      } else {
        handleError(e);
      }
    }
  };

  return (
    <Modal
      open={open}
      className="modern-modal"
      onCancel={onCancel}
      onOk={handleOk}
      okText="Update Password"
      title="Change Password"
      destroyOnHidden
      centered
      width="90vw"
      style={{ maxWidth: 500 }}
    >
      <Form form={form} layout="vertical" requiredMark="optional">
        <Divider style={{ margin: '24px 0' }}>
          <Text type="secondary" style={{ fontSize: '12px' }}>
            CURRENT PASSWORD
          </Text>
        </Divider>

        <Row gutter={16}>
          <Col xs={24} sm={12}>
            <AntdFormItem
              name="oldPassword"
              label="Current Password"
              type="password"
              rules={[{ required: true, message: 'Current password is required' }]}
              placeholder="Enter current password"
            />
          </Col>
        </Row>

        <Divider style={{ margin: '24px 0' }}>
          <Text type="secondary" style={{ fontSize: '12px' }}>
            NEW PASSWORD
          </Text>
        </Divider>

        <Row gutter={16}>
          <Col xs={24} sm={12}>
            <AntdFormItem
              name="newPassword"
              label="New Password"
              type="password"
              rules={[
                { required: true, message: 'New password is required' },
                { min: 5, message: 'Password must be at least 5 characters' },
              ]}
              placeholder="Enter new password"
            />
          </Col>
        </Row>

        <Row gutter={16}>
          <Col xs={24} sm={12}>
            <AntdFormItem
              name="confirmPassword"
              label="Confirm New Password"
              type="password"
              dependencies={['newPassword']}
              rules={[
                { required: true, message: 'Confirm new password' },
                ({ getFieldValue }) => ({
                  validator(_, value) {
                    if (!value || getFieldValue('newPassword') === value) {
                      return Promise.resolve();
                    }
                    return Promise.reject(new Error('Passwords do not match'));
                  },
                }),
              ]}
              placeholder="Confirm new password"
            />
          </Col>
        </Row>

        {globalMessages && <MessageBanner messages={globalMessages} />}
      </Form>
    </Modal>
  );
}