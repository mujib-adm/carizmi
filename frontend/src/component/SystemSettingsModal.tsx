import { Form, Modal } from "antd";
import { useEffect } from "react";
import { SystemSetting } from "../constants/types";
import { useApiMessages } from "../hook/ApiResponseHandler";
import { AntdFormItem } from "./AntdFormItem";
import { MessageBanner } from "./MessageBanner";

type Props = {
  open: boolean;
  onCancel: () => void;
  onSubmit: (values: SystemSetting) => Promise<void>;
  initial?: SystemSetting | null;
};

export function SystemSettingsModal({ open, onCancel, onSubmit, initial }: Props) {
  const [form] = Form.useForm();

  const { globalMessages, handleError, resetMessages } = useApiMessages<SystemSetting>(
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
      console.error("Error submitting system setting form. SystemSettingsModal.submit: ", e);
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
      destroyOnClose
      centered
      width={450}
    >
      <Form form={form} layout="vertical" className="member-form" requiredMark={false}>
        <AntdFormItem 
          name="settingType" 
          label="Type" 
          placeholder="Type" 
          disabled 
        />
        
        <AntdFormItem 
          name="settingKey" 
          label="Key" 
          placeholder="Key" 
          disabled 
        />
        
        <AntdFormItem 
          name="settingValue" 
          label="Value" 
          rules={[{ required: true, message: "Value is required" }]} 
          placeholder="Value" 
        />

        {globalMessages && <MessageBanner messages={globalMessages} />}
      </Form>
    </Modal>
  );
}