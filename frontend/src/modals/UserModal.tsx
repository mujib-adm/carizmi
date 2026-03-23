import { Form, Modal, Select, Switch } from 'antd';
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
    >
      <Form form={form} layout="vertical">
        <AntdFormItem name="firstName" label="First Name" disabled={true} />
        <AntdFormItem name="lastName" label="Last Name" disabled={true} />
        <AntdFormItem name="email" label="Email" disabled={true} />

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
        <Form.Item name="active" label="Status" valuePropName="checked">
          <Switch checkedChildren="Active" unCheckedChildren="Inactive" />
        </Form.Item>

        {globalMessages && <MessageBanner messages={globalMessages} />}
      </Form>
    </Modal>
  );
};