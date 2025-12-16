import { Navigate } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import "../../themes/css/auth-form.css";
import { useForm } from "react-hook-form";

import apiClient from "../../apiclient/ApiClient";
import { GlobalResponse, MessageType, RegisterForm } from "../../constants/types";
import { useNotification } from "../../context/NotificationContext";
import { FormField } from "../../component/FormField";
import { useApiMessages } from "../../hook/ApiResponseHandler";
import { MessageBanner } from "../../component/MessageBanner";

export default function Register() {
  const { token, isLoading } = useAuth();
  const notify = useNotification();

  const { register, handleSubmit, setError, formState: { errors } } = useForm<RegisterForm>();
  const { globalMessages, handleResponse, handleError, resetMessages } = useApiMessages<RegisterForm>(setError);

  const onSubmit = async (formValues: RegisterForm) => {
    try {
      // clear old messages from MessageBanner before starting new call
      resetMessages();
      const response = await apiClient.post<GlobalResponse>("/auth/register", formValues);
      const responseBody = response.data;

      if (responseBody.globalMessages?.length > 0) {
        const msg = responseBody.globalMessages[0];
        if (msg.type === MessageType.SUCCESS) {
          notify.success({ message: "Success", description: msg.message }, "/login");
        } else {
          handleResponse(responseBody);
        }
      }
    } catch (error: any) {
      handleError(error);
    }
  };

  if (!isLoading && token) return <Navigate to="/" />;

  return (
    <div className="auth-container">
      <form className="auth-form" onSubmit={handleSubmit(onSubmit)}>
        <h2 style={{ marginBottom: "30px" }}>Register</h2>


        <div className="form_label"><label className="text_uppercase"> First Name <span className="required_filed">*</span> </label></div>
        <FormField type="text" placeholder="Enter first name..." registerProps={register("firstName", { required: "First Name is required" })} error={errors.firstName} />

        <div className="form_label"><label className="text_uppercase"> Last Name <span className="required_filed">*</span> </label></div>
        <FormField type="text" placeholder="Enter last name..." registerProps={register("lastName", { required: "Last Name is required" })} error={errors.lastName} />

        <div className="form_label"><label className="text_uppercase"> Email <span className="required_filed">*</span> </label></div>
        <FormField type="email" placeholder="Enter email..." registerProps={register("email", { required: "Email is required" })} error={errors.email} />

        <div className="form_label"><label className="text_uppercase"> Username <span className="required_filed">*</span> </label></div>
        <FormField type="text" placeholder="Enter username..." registerProps={register("username", { required: "Username is required" })} error={errors.username} />

        <div className="form_label"><label className="text_uppercase"> Password <span className="required_filed">*</span> </label></div>
        <FormField type="password" placeholder="Enter password..." registerProps={register("password", { required: "Password is required" })} error={errors.password} />

        {globalMessages && <MessageBanner messages={globalMessages} />}

        <button className="global_btn" type="submit"> Register </button>
        <p className="auth-link"> Already have an account? <a href="/login">Login here</a> </p>
      </form>
    </div>
  );
}