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
        <h2>Sign Up</h2>

        <FormField type="text" placeholder="First Name" registerProps={register("firstName", { required: "First Name is required" })} error={errors.firstName} />

        <FormField type="text" placeholder="Last Name" registerProps={register("lastName", { required: "Last Name is required" })} error={errors.lastName} />

        <FormField type="email" placeholder="Email" registerProps={register("email", { required: "Email is required" })} error={errors.email} />

        <FormField type="text" placeholder="Username" registerProps={register("username", { required: "Username is required" })} error={errors.username} />

        <FormField type="password" placeholder="Password" registerProps={register("password", { required: "Password is required" })} error={errors.password} />

        {globalMessages && <MessageBanner messages={globalMessages} />}

        <button className="global_btn" type="submit"> Register </button>
        <p className="auth-link"> Already have an account? <a href="/login">Login here</a> </p>
      </form>
    </div>
  );
}