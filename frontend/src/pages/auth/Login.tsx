import { useNavigate } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import "../../themes/css/auth-form.css";
import { useForm } from "react-hook-form";

import apiClient from "../../apiclient/ApiClient";
import { GlobalResponse, LoginData, LoginForm } from "../../constants/types";
import { MessageBanner } from "../../component/MessageBanner";
import { FormField } from "../../component/FormField";
import { useApiMessages } from "../../hook/ApiResponseHandler";

export default function Login() {
  const navigate = useNavigate();
  const { login } = useAuth();

  const { register, handleSubmit, formState: { errors }, } = useForm<LoginForm>();
  const { globalMessages, handleResponse, handleError, resetMessages } = useApiMessages<LoginForm>();

  const onSubmit = async (formValues: LoginForm) => {
    // clear old messages from MessageBanner before starting new call
    resetMessages();
    try {
      const response = await apiClient.post<GlobalResponse<LoginData>>("/auth/login", formValues);
      const responseBody = response.data;

      if (responseBody?.map?.token && responseBody?.map?.role) {
        login(responseBody.map.token, responseBody.map.role);
        navigate("/");
      } else if (responseBody.globalMessages?.length > 0) {
        handleResponse(responseBody);
      } else {
        console.error("Login response missing a token and/or role");
      }
    } catch (error: any) {
      handleError(error);
    }
  };

  return (
    <div className="auth-container">
      <form className="auth-form" onSubmit={handleSubmit(onSubmit)}>
        <h2>Login</h2>
        <FormField type="text" placeholder="Username" registerProps={register("username", { required: "Username is required" })} error={errors.username} />
        <FormField type="password" placeholder="Password" registerProps={register("password", { required: "Password is required" })} error={errors.password} />
        {globalMessages && <MessageBanner messages={globalMessages} />}

        <button className="global_btn" type="submit"> Login </button>
        <p className="auth-link"> Don't have an account? <a href="/register">Register here</a> </p>
      </form>
    </div>
  );
}