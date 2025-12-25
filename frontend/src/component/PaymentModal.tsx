import { Col, Divider, Form, Modal, Row } from 'antd';
import dayjs from 'dayjs';
import { useEffect } from 'react';
import { Payment, SystemSetting } from '../constants/types';
import { useApiMessages } from '../hook/ApiResponseHandler';
import { AntdFormItem } from './AntdFormItem';
import MemberLookup from './MemberLookup';
import { MessageBanner } from './MessageBanner';

interface PaymentModalProps {
    open: boolean;
    onCancel: () => void;
    onSubmit: (values: any) => Promise<void>;
    initialValues?: Payment | null;
    feeTypes: SystemSetting[];
    paymentMethods: SystemSetting[];
}

export default function PaymentModal({ open, onCancel, onSubmit, initialValues, feeTypes, paymentMethods }: PaymentModalProps) {
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
                    dateReceived: initialValues.dateReceived ? dayjs(initialValues.dateReceived) : null,
                    year: initialValues.year,
                    quarter: initialValues.quarter
                });
            } else {
                form.resetFields();
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
                quarter: values.quarter
            };

            await onSubmit(payload);
        } catch (e: any) {
            console.error("Validation failed or submit error", e);
            if (e.errorFields) {
                // Ant Design form validation error, do nothing
            } else {
                handleError(e);
            }
        }
    };

    const title = initialValues?.memberID ? "Edit Payment" : "Add Payment";

    return (
        <Modal
            open={open}
            onCancel={onCancel}
            onOk={handleOk}
            okText="Save"
            title={
                <div style={{ marginBottom: "30px", textAlign: "center", width: "100%", fontSize: "1.5rem" }}>
                    {title}
                </div>
            }
            destroyOnHidden
        >
            <Form form={form} layout="vertical" >
                <Form.Item name="memberID" label="Member" rules={[{ required: true }]}>
                    <MemberLookup onSelectMember={(m) => {
                        // Optional side effects
                    }} />
                </Form.Item>
                <Divider />

                <Row gutter={16}>
                    <Col span={12}> <AntdFormItem name="feeType" label="Fee Type" type="select" rules={[{ required: true }]} options={feeTypes.map(f => ({ value: f.settingValue, label: f.settingValue }))} /> </Col>
                    <Col span={12}> <AntdFormItem name="amount" label="Amount" type="number" rules={[{ required: true }]} inputProps={{ prefix: "$" }} /> </Col>
                </Row>

                <Form.Item noStyle shouldUpdate={(prev, curr) => prev.feeType !== curr.feeType}>
                    {({ getFieldValue }) => {
                        return getFieldValue('feeType') === 'Membership Fee' ? (
                            <Row gutter={16}>
                                <Col span={12}> <AntdFormItem name="year" label="Year" type="select" rules={[{ required: true }]} initialValue={dayjs().year()} options={[0, 1, 2].map(i => { const y = dayjs().year() - 1 + i; return { value: y, label: y }; })} /> </Col>
                                <Col span={12}> <AntdFormItem name="quarter" label="Quarter" type="select" rules={[{ required: true }]} options={[1, 2, 3, 4].map(q => ({ value: q, label: `Q${q}` }))} /> </Col>
                            </Row>
                        ) : null;
                    }}
                </Form.Item>

                <Row gutter={16}>
                    <Col span={12}> <AntdFormItem name="dateReceived" label="Date Received" type="date" rules={[{ required: true }]} /> </Col>
                    <Col span={12}> <AntdFormItem name="methodOfPayment" label="Payment Method" type="select" rules={[{ required: true }]} options={paymentMethods.map(p => ({ value: p.settingValue, label: p.settingValue }))} /> </Col>
                </Row>

                {globalMessages && <MessageBanner messages={globalMessages} />}

            </Form>
        </Modal>
    );
};