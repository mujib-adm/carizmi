import { Col, Form, Modal, Row, Select, Switch } from 'antd';
import { useEffect } from 'react';
import { RoleConstants } from '../constants/RoleConstants';
import { UserResponseDto } from '../api/generated/types';
import { AntdFormItem } from '../component/AntdFormItem';
import { MessageBanner } from '../component/MessageBanner';
import { useApiMessages } from '../hook/ApiResponseHandler';

interface UserModalProps {
  open: boolean;
  onCancel: () => void;
  onSubmit: (values: UserResponseDto) => Promise<void>;
  initialValues?: UserResponseDto | null;
}

export const UserModal = ({ open, onCancel, onSubmit, initialValues }: UserModalProps) => {
  const [form] = Form.useForm();

  const { globalMessages, handleError, resetMessages } = useApiMessages();

  useEffect(() => {
    if (open && initialValues) {
      resetMessages();
      form.setFieldsValue(initialValues);
    } else {
      form.resetFields();
    }
  }, [open, initialValues, form, resetMessages]);

  const handleOk = async () => {
    try {
      resetMessages();
      const values = await form.validateFields();
      await onSubmit({ ...initialValues, ...values });
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
      title="Edit User"
      onCancel={onCancel}
      onOk={handleOk}
      okText="Save"
      className="modern-modal"
      centered
      destroyOnHidden
      width="90vw"
      style={{ maxWidth: 500 }}
    >
      <Form form={form} layout="vertical">
        <Row gutter={16}>
          <Col xs={24} sm={12}>
            <AntdFormItem name="firstName" label="First Name" disabled={true} />
          </Col>
        </Row>
        <Row gutter={16}>
          <Col xs={24} sm={12}>
            <AntdFormItem name="lastName" label="Last Name" disabled={true} />
          </Col>
        </Row>
        <Row gutter={16}>
          <Col xs={24} sm={12}>
            <AntdFormItem name="email" label="Email" disabled={true} />
          </Col>
        </Row>

        <Row gutter={16}>
          <Col xs={24} sm={12}>
            <AntdFormItem
              name="role"
              label="Role"
              type="select"
              options={Object.values(RoleConstants).map((role) => ({
                value: role,
                label: role.charAt(0).toUpperCase() + role.slice(1).toLowerCase(),
              }))}
              rules={[{ required: true, message: 'Please select a role' }]}
            />
          </Col>
        </Row>
        <Row gutter={16}>
          <Col xs={24} sm={12}>
            <Form.Item name="active" label="Status" valuePropName="checked">
              <Switch checkedChildren="Active" unCheckedChildren="Inactive" />
            </Form.Item>
          </Col>
        </Row>

        {globalMessages && <MessageBanner messages={globalMessages} />}
      </Form>
    </Modal>
  );
};