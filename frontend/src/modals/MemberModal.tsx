import { Col, Form, Modal, Row } from "antd";
import dayjs from "dayjs";
import { useEffect } from "react";
import { ReferenceConstants } from "../constants/ReferenceConstants";
import { Member, MemberRequestDto } from "../constants/types";
import { useApiMessages } from "../hook/ApiResponseHandler";
import "../themes/css/member.css";
import { AntdFormItem } from "../component/AntdFormItem";
import { MessageBanner } from "../component/MessageBanner";

type Props = {
  open: boolean;
  onCancel: () => void;
  onSubmit: (values: MemberRequestDto) => Promise<void>;
  initial?: Member | null;
  statusOptions: { value: string; label: string }[];
};

export function MemberModal({ open, onCancel, onSubmit, initial, statusOptions }: Props) {
  const [form] = Form.useForm();
  
  const { globalMessages, handleError, resetMessages } = useApiMessages<MemberRequestDto>(
    undefined,
    (field, msg) => {
      form.setFields([{ name: field, errors: [msg] }]);
    }
  );

  useEffect(() => {
    if (open) {
      resetMessages();
      if (initial) {
        form.setFieldsValue({
          ...initial,
          joinDate: initial.joinDate ? dayjs(initial.joinDate) : null,
        });
      } else {
        form.resetFields();
        form.setFieldsValue({
          status: ReferenceConstants.MEMBER_STATUS.ACTIVE,
          joinDate: dayjs(),
          state: "MN",
        });
      }
    }
  }, [initial, open, form, resetMessages]);

  const handleOk = async () => {
    try {
      resetMessages();
      const values = await form.validateFields();
      
      const payload: MemberRequestDto = {
        ...values,
        joinDate: values.joinDate ? values.joinDate.format("YYYY-MM-DD") : null,
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

  const title = initial?.memberID ? "Edit Member" : "Add Member";

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
      width={520}
    >
      <Form form={form} layout="vertical" className="member-form" requiredMark={false}>
        <Row gutter={12}>
          <Col span={12}>
            <AntdFormItem name="firstName" label="First Name" rules={[{ required: true, message: "First Name is required" }]} placeholder="First Name" />
          </Col>
          <Col span={12}>
            <AntdFormItem name="lastName" label="Last Name" rules={[{ required: true, message: "Last Name is required" }]} placeholder="Last Name" />
          </Col>
        </Row>

        <Row gutter={12}>
          <Col span={12}>
            <AntdFormItem name="phone" label="Phone" type="tel" rules={[{ required: true, message: "Phone is required" }]} placeholder="Phone" />
          </Col>
          <Col span={12}>
            <AntdFormItem name="email" label="Email" type="email" placeholder="Email" />
          </Col>
        </Row>

        <Row gutter={12}>
          <Col span={12}>
            <AntdFormItem name="status" label="Status" type="select" options={statusOptions} rules={[{ required: true, message: "Status is required" }]} placeholder="Pick status" />
          </Col>
          <Col span={12}>
            <AntdFormItem name="joinDate" label="Join Date" type="date" rules={[{ required: true, message: "Join Date is required" }]} placeholder="mm/dd/yyyy" />
          </Col>
        </Row>

        <AntdFormItem name="address1" label="Address 1" placeholder="Address 1" />
        <AntdFormItem name="address2" label="Address 2" placeholder="Address 2" />

        <Row gutter={12}>
          <Col span={14}>
            <AntdFormItem name="city" label="City" placeholder="City" />
          </Col>
          <Col span={5}>
            <AntdFormItem name="state" label="State" rules={[{ required: true, message: "State is required" }]} placeholder="MN" />
          </Col>
          <Col span={5}>
            <AntdFormItem name="zip" label="ZIP" placeholder="ZIP" />
          </Col>
        </Row>

        {globalMessages && <MessageBanner messages={globalMessages} />}
      </Form>
    </Modal>
  );
}