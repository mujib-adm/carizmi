import { Col, Divider, Form, Modal, Row } from 'antd';
import dayjs from 'dayjs';
import { useEffect } from 'react';
import { ReferenceConstants } from '../constants/ReferenceConstants';
import { SystemSettingConstants } from '../constants/SystemSettingsConstants.ts';
import { PaymentDto } from '../api/generated/types';
import { useSystemSettings } from '../hooks/useSystemSettings';
import { useApiMessages } from '../hooks/useApiMessages';
import { AntdFormItem } from '../components/AntdFormItem';
import MemberLookup from '../components/MemberLookup';
import { MessageBanner } from '../components/MessageBanner';

interface PaymentModalProps {
  open: boolean;
  onCancel: () => void;
  onSubmit: (values: PaymentDto) => Promise<void>;
  initialValues?: PaymentDto | null;
  feeTypes: { value: string; label: string }[];
  paymentMethods: { value: string; label: string }[];
}

export default function PaymentModal({
  open,
  onCancel,
  onSubmit,
  initialValues,
  feeTypes,
  paymentMethods,
}: PaymentModalProps) {
  const [form] = Form.useForm();
  const { getNumericSetting } = useSystemSettings();

  const { globalMessages, handleError, resetMessages } = useApiMessages(undefined, (field, msg) => {
    form.setFields([{ name: field, errors: [msg] }]);
  });

  useEffect(() => {
    if (open) {
      resetMessages();
      if (initialValues) {
        form.setFieldsValue({
          ...initialValues,
          dateReceived: initialValues.dateReceived ? dayjs(initialValues.dateReceived) : null,
          year: initialValues.year,
          quarter: initialValues.quarter,
        });
      } else {
        form.resetFields();
        form.setFieldsValue({
          feeType: ReferenceConstants.FEE_TYPE.MEMBERSHIP_FEE,
          amount: getNumericSetting(
            SystemSettingConstants.FEE.TYPE,
            SystemSettingConstants.FEE.MEMBERSHIP_FEE
          ),
          dateReceived: dayjs(),
          quarter: Math.floor(dayjs().month() / 3) + 1,
        });
      }
    }
  }, [open, initialValues, form, resetMessages]);

  const handleOk = async () => {
    try {
      resetMessages();
      const values = await form.validateFields();

      const payload = {
        ...values,
        dateReceived: values.dateReceived ? values.dateReceived.format('YYYY-MM-DD') : null,
        paymentID: initialValues?.paymentID,
        year: values.year,
        quarter: values.quarter,
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

  const title = initialValues?.paymentID ? 'Edit Payment' : 'Add Payment';

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
      width="90vw"
      style={{ maxWidth: 500 }}
    >
      <Form
        form={form}
        layout="vertical"
        onValuesChange={(changedValues) => {
          if (changedValues.feeType) {
            let amount = 0;
            if (changedValues.feeType === ReferenceConstants.FEE_TYPE.MEMBERSHIP_FEE) {
              amount = getNumericSetting(
                SystemSettingConstants.FEE.TYPE,
                SystemSettingConstants.FEE.MEMBERSHIP_FEE
              );
            } else if (changedValues.feeType === ReferenceConstants.FEE_TYPE.REGISTRATION_FEE) {
              amount = getNumericSetting(
                SystemSettingConstants.FEE.TYPE,
                SystemSettingConstants.FEE.REGISTRATION_FEE
              );
            }
            if (amount > 0) {
              form.setFieldsValue({ amount });
            }
          }
        }}
      >
        <Form.Item name="memberID" label="Member" rules={[{ required: true }]}>
          <MemberLookup
            onSelectMember={(m) => {
              // Optional side effects
            }}
            onError={handleError}
          />
        </Form.Item>
        <Divider />

        <Row gutter={16}>
          <Col xs={24} sm={12}>
            <AntdFormItem
              name="feeType"
              label="Fee Type"
              type="select"
              rules={[{ required: true }]}
              options={feeTypes}
            />
          </Col>
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

        <Form.Item noStyle shouldUpdate={(prev, curr) => prev.feeType !== curr.feeType}>
          {({ getFieldValue }) => {
            return getFieldValue('feeType') === ReferenceConstants.FEE_TYPE.MEMBERSHIP_FEE ? (
              <Row gutter={16}>
                <Col xs={24} sm={12}>
                  <AntdFormItem
                    name="year"
                    label="Year"
                    type="select"
                    rules={[{ required: true }]}
                    initialValue={dayjs().year()}
                    options={[0, 1, 2].map((i) => {
                      const y = dayjs().year() - 1 + i;
                      return { value: y, label: y };
                    })}
                  />
                </Col>
                <Col xs={24} sm={12}>
                  <AntdFormItem
                    name="quarter"
                    label="Quarter"
                    type="select"
                    rules={[{ required: true }]}
                    options={[1, 2, 3, 4].map((q) => ({ value: q, label: `Q${q}` }))}
                  />
                </Col>
              </Row>
            ) : null;
          }}
        </Form.Item>

        <Row gutter={16}>
          <Col xs={24} sm={12}>
            <AntdFormItem
              name="dateReceived"
              label="Date Received"
              type="date"
              rules={[{ required: true }]}
            />
          </Col>
          <Col xs={24} sm={12}>
            <AntdFormItem
              name="methodOfPayment"
              label="Payment Method"
              type="select"
              rules={[{ required: true }]}
              options={paymentMethods}
            />
          </Col>
        </Row>

        {globalMessages && <MessageBanner messages={globalMessages} />}
      </Form>
    </Modal>
  );
}