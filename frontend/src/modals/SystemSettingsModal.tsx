import { Col, Form, Modal, Row } from 'antd';
import { useEffect } from 'react';
import { SystemSettingsDto } from '../api/generated/types';
import { useApiMessages } from '../hook/ApiResponseHandler';
import { AntdFormItem } from '../component/AntdFormItem';
import { MessageBanner } from '../component/MessageBanner';

type Props = {
  open: boolean;
  onCancel: () => void;
  onSubmit: (values: SystemSettingsDto) => Promise<void>;
  initial?: SystemSettingsDto | null;
};

export function SystemSettingsModal({ open, onCancel, onSubmit, initial }: Props) {
  const [form] = Form.useForm();

  const { globalMessages, handleError, resetMessages } = useApiMessages<SystemSettingsDto>(
    undefined,
    (field, msg) => {
      form.setFields([{ name: field as string, errors: [msg] }]);
    }
  );

  useEffect(() => {
    if (open) {
      resetMessages();
      if (initial) {
        form.setFieldsValue(initial);
      } else {
        form.resetFields();
      }
    }
  }, [initial, open, form, resetMessages]);

  const handleOk = async () => {
    try {
      resetMessages();
      const values = await form.validateFields();

      const payload = {
        ...initial,
        ...values,
      };

      await onSubmit(payload);
      onCancel();
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
      okText="Save"
      title="Edit Setting"
      destroyOnHidden
      centered
      width="90vw"
      style={{ maxWidth: 500 }}
    >
      <Form form={form} layout="vertical" className="member-form" requiredMark={false}>
        <Row gutter={16}>
          <Col xs={24} sm={12}>
            <AntdFormItem name="settingName" label="Name" placeholder="Name" disabled />
          </Col>
        </Row>

        <Row gutter={16}>
          <Col xs={24} sm={12}>
            <AntdFormItem name="settingKey" label="Key" placeholder="Key" disabled />
          </Col>
        </Row>

        <Row gutter={16}>
          <Col xs={24} sm={12}>
            <AntdFormItem
              name="settingValue"
              label="Value"
              rules={[{ required: true, message: 'Value is required' }]}
              placeholder="Value"
            />
          </Col>
        </Row>

        {globalMessages && <MessageBanner messages={globalMessages} />}
      </Form>
    </Modal>
  );
}