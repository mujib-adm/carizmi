import { Col, Form, Input, Modal, Row } from 'antd';
import dayjs from 'dayjs';
import { useEffect } from 'react';
import { Expense } from '../constants/types';
import { useApiMessages } from '../hook/ApiResponseHandler';
import { AntdFormItem } from '../component/AntdFormItem';
import { MessageBanner } from '../component/MessageBanner';

interface ExpenseModalProps {
  open: boolean;
  onCancel: () => void;
  onSubmit: (values: any) => Promise<void>;
  initialValues?: Expense | null;
  categories: { value: string; label: string }[];
}

export default function ExpenseModal({ open, onCancel, onSubmit, initialValues, categories}: ExpenseModalProps) {
    const [form] = Form.useForm();
    const { globalMessages, handleError, resetMessages } = useApiMessages(
        undefined,
        (field, msg) => {
            form.setFields([{ name: field, errors: [msg] }]);
        }
    );

  useEffect(() => {
    if (open) {
      resetMessages();
      if (initialValues) {
        form.setFieldsValue({
          ...initialValues,
          dateOfExpense: initialValues.dateOfExpense ? dayjs(initialValues.dateOfExpense) : null,
        });
      } else {
        form.resetFields();
        form.setFieldsValue({ dateOfExpense: dayjs() });
      }
    }
  }, [open, initialValues, form, resetMessages]);

  const handleOk = async () => {
    try {
      resetMessages();
      const values = await form.validateFields();

      const payload = {
        ...values,
        dateOfExpense: values.dateOfExpense ? values.dateOfExpense.format('YYYY-MM-DD') : null,
        expenseID: initialValues?.expenseID,
      };

      await onSubmit(payload);
    } catch (e: any) {
      if (e.errorFields) {
        // Ant Design form validation error, do nothing
      } else {
        handleError(e);
      }
    }
  };

  const title = initialValues?.expenseID ? 'Edit Expense' : 'Add Expense';

  return (
    <Modal
      open={open}
      className="modern-modal"
      onCancel={onCancel}
      onOk={handleOk}
      okText="Save"
      title={title}
      destroyOnHidden
      centered
    >
      <Form form={form} layout="vertical">
        <Row gutter={16}>
          <Col xs={24} sm={12}>
            <AntdFormItem
              name="category"
              label="Category"
              type="select"
              rules={[{ required: true }]}
              options={categories}
            />
          </Col>
        </Row>
        <Row gutter={16}>
          <Col xs={24} sm={12}>
            <AntdFormItem
              name="amount"
              label="Amount"
              type="number"
              rules={[{ required: true }]}
              inputProps={{ prefix: '$' }}
            />
          </Col>
        </Row>
        <Row gutter={16}>
          <Col xs={24} sm={12}>
            <AntdFormItem
              name="dateOfExpense"
              label="Date of Expense"
              type="date"
              rules={[{ required: true }]}
            />
          </Col>
        </Row>
        <Row gutter={16}>
          <Col xs={24}>
            <Form.Item name="description" label="Description" rules={[{ required: true }]}>
              <Input.TextArea rows={3} placeholder="Describe the expense..." />
            </Form.Item>
          </Col>
        </Row>

        {globalMessages && <MessageBanner messages={globalMessages} />}
      </Form>
    </Modal>
  );
}