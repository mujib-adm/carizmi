import { Modal } from "antd";
import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { Member, MemberRequestDto } from "../constants/types";
import { useApiMessages } from "../hook/ApiResponseHandler";
import "../themes/css/member.css";
import { MessageBanner } from "./MessageBanner";
import { ModalField } from "./ModalField";

type Props = {
  open: boolean;
  onCancel: () => void;
  onSubmit: (values: MemberRequestDto) => Promise<void>;
  initial?: Member | null;
  statusOptions: { value: string; label: string }[];
};

export function MemberModal({ open, onCancel, onSubmit, initial, statusOptions }: Props) {
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
      onCancel();
    } catch (e: any) {
      console.error("Error submitting member form. MemberModal.submit: ", e);
      handleError(e);
    }
  };

  const title = initial?.memberID ? "Edit Member" : "Add Member";

  return (
    <Modal
      open={open}
      className="modern-modal"
      onCancel={onCancel}
      onOk={handleSubmit(submit)}
      okText="Save"
      title={title}
      destroyOnHidden
      centered
    >
      <form className="member-form" onSubmit={handleSubmit(submit)}>
        <div className="two-col">
          <ModalField as="input" type="text" name="firstName" label="First Name" placeholder="First Name" registerProps={register("firstName", { required: "First Name is required" })} error={errors.firstName} />
          <ModalField as="input" type="text" name="lastName" label="Last Name" placeholder="Last Name" registerProps={register("lastName", { required: "Last Name is required" })} error={errors.lastName} />
        </div>

        <div className="two-col">
          <ModalField as="input" type="text" name="phone" label="Phone" placeholder="Phone" registerProps={register("phone", { required: "Phone is required" })} error={errors.phone} />
          <ModalField as="input" type="email" name="email" label="Email" placeholder="Email" registerProps={register("email")} error={errors.email} />
        </div>

        <div className="two-col">
          <ModalField as="select" name="status" label="Status" control={control} placeholder="Pick status" options={statusOptions} registerProps={register("status", { required: "Status is required" })} error={errors.status} />
          <ModalField as="date" name="joinDate" label="Join Date" control={control} placeholder="mm/dd/yyyy" registerProps={register("joinDate", { required: "Join Date is required" })} error={errors.joinDate} />
        </div>

        <ModalField as="input" type="text" name="address1" label="Address 1" placeholder="Address 1" registerProps={register("address1")} error={errors.address1} />
        <ModalField as="input" type="text" name="address2" label="Address 2" placeholder="Address 2" registerProps={register("address2")} error={errors.address2} />

        <div className="three-col">
          <ModalField as="input" type="text" name="city" label="City" placeholder="City" registerProps={register("city")} error={errors.city} />
          <ModalField as="input" type="text" name="state" label="State" placeholder="State" registerProps={register("state", { required: "State is required" })} error={errors.state} />
          <ModalField as="input" type="text" name="zip" label="ZIP" placeholder="ZIP" registerProps={register("zip")} error={errors.zip} />
        </div>

        {globalMessages && <MessageBanner messages={globalMessages} />}

      </form>
    </Modal>
  );
};