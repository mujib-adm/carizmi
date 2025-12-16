import React, { useEffect } from "react";
import { Modal } from "antd";
import { useForm } from "react-hook-form";
import { MessageBanner } from "./MessageBanner";
import { useApiMessages } from "../hook/ApiResponseHandler";
import "../themes/css/member.css";
import { MemberRequestDto, Member } from "../constants/types";
import { FormFieldModal } from "./FormFieldModal";

type Props = {
  open: boolean;
  onClose: () => void;
  onSubmit: (values: MemberRequestDto) => Promise<void>;
  initial?: Member | null;
  statusOptions: { value: string; label: string }[];
};

export const MemberFormModal: React.FC<Props> = ({ open, onClose, onSubmit, initial, statusOptions }) => {
  const { control, register, handleSubmit, setError, reset, formState: { errors } } = useForm<MemberRequestDto>({
    defaultValues: initial ?? { state: "MN", status: "" }
  });

  const { globalMessages, handleError, resetMessages } = useApiMessages<MemberRequestDto>(setError);

  useEffect(() => {
    reset(initial ?? { state: "MN", status: "" });
  }, [initial, open, reset]);

  const submit = async (values: MemberRequestDto) => {
    try {
      resetMessages();
      await onSubmit(values);
      onClose();
    } catch (e: any) {
      console.error("Error submitting member form. MemberFormModal.submit: ", e);
      handleError(e);
    }
  };

  const title = initial?.memberID ? `Edit Member #${initial.memberID}` : "Add Member";

  return (
    // <Modal open={open} onCancel={onClose} onOk={handleSubmit(submit)} okText="Save" cancelText="Cancel" title={title} destroyOnHidden>
    <Modal
      open={open}
      onCancel={onClose}
      onOk={handleSubmit(submit)}
      okText="Save"
      cancelText="Cancel"
      title={
        <div style={{ marginBottom: "30px", textAlign: "center", width: "100%", fontSize: "1.5rem" }}>
          {title}
        </div>
      }
      destroyOnHidden
    >
      <form className="member-form" onSubmit={handleSubmit(submit)}>
        <div className="two-col">
          <FormFieldModal as="input" type="text" name="firstName" label="First Name" placeholder="First Name" registerProps={register("firstName", { required: "First Name is required" })} error={errors.firstName} />
          <FormFieldModal as="input" type="text" name="lastName" label="Last Name" placeholder="Last Name" registerProps={register("lastName", { required: "Last Name is required" })} error={errors.lastName} />
        </div>

        <div className="two-col">
          <FormFieldModal as="input" type="text" name="phone" label="Phone" placeholder="Phone" registerProps={register("phone", { required: "Phone is required" })} error={errors.phone} />
          <FormFieldModal as="input" type="email" name="email" label="Email" placeholder="Email" registerProps={register("email")} error={errors.email} />
        </div>

        <div className="two-col">
          <FormFieldModal as="select" name="status" label="Status" control={control} placeholder="Pick status" options={statusOptions} registerProps={register("status", { required: "Status is required" })} error={errors.status} />
          <FormFieldModal as="date" name="joinDate" label="Join Date" control={control} placeholder="mm/dd/yyyy" registerProps={register("joinDate", { required: "Join Date is required" })} error={errors.joinDate} />
        </div>

        <FormFieldModal as="input" type="text" name="address1" label="Address 1" placeholder="Address 1" registerProps={register("address1")} error={errors.address1} />
        <FormFieldModal as="input" type="text" name="address2" label="Address 2" placeholder="Address 2" registerProps={register("address2")} error={errors.address2} />

        <div className="three-col">
          <FormFieldModal as="input" type="text" name="city" label="City" placeholder="City" registerProps={register("city")} error={errors.city} />
          <FormFieldModal as="input" type="text" name="state" label="State" placeholder="State" registerProps={register("state", { required: "State is required" })} error={errors.state} />
          <FormFieldModal as="input" type="text" name="zip" label="ZIP" placeholder="ZIP" registerProps={register("zip")} error={errors.zip} />
        </div>

        {globalMessages && <MessageBanner messages={globalMessages} />}

      </form>
    </Modal>
  );
};