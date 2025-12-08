import { useNavigate } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import "../../themes/css/auth-form.css";
import { useForm } from "react-hook-form";

import apiClient from "../../context/ApiClient";
import { normalizeAxiosResponse } from "../../context/ErrorUtils";
import { GlobalResponse, LoginData, LoginForm } from "../../constants/types";
import React from "react";

export default function Login() {
  const navigate = useNavigate();
  const { login } = useAuth();

  const {
    register,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<LoginForm>();

  const [globalError, setGlobalError] = React.useState("");

  const onSubmit = async (formValues: LoginForm) => {
    setGlobalError("");

    try {
      const response = await apiClient.post<GlobalResponse<LoginData>>("/auth/login", formValues);
      const responseBody = response.data;
      if (responseBody.map?.token && responseBody.map?.role) {
        login(responseBody.map?.token, responseBody.map?.role);
        navigate("/");
      } else {
        setGlobalError(responseBody.globalMessages?.[0]?.message || "Login failed. Please try again or contact support.");
      }
      console.log("token: ", responseBody.map?.token); //TODO: remove in production
    } catch (error: any) {
      // AxiosError<GlobalResponse>
      const backendResponse: GlobalResponse | undefined = error.response?.data;
      if (backendResponse) {
        const normalized = normalizeAxiosResponse(backendResponse);
        // Map fieldMessages → RHF errors
        Object.entries(normalized.fieldMessages).forEach(([field, message]) => {
          setError(field as keyof LoginForm, { type: "server", message });
        });
        // Global errors → banner
        if (normalized.globalMessages.length > 0) {
          setGlobalError(normalized.globalMessages[0]);
        } else {
          setGlobalError(error.message || "Login failed. Please try again or contact support.");
        }
      }

    }
  };

  return (
    <div className="auth-container">
      <form className="auth-form" onSubmit={handleSubmit(onSubmit)}>

        <h2>Login</h2>

        <input className="auth-input" type="text" placeholder="Username" {...register("username", { required: "Username is required" })} />
        {errors.username && (<p className="auth-error">{errors.username.message}</p>)}

        <input className="auth-input" type="password" placeholder="Password" {...register("password", { required: "Password is required" })} />
        {errors.password && (<p className="auth-error">{errors.password.message}</p>)}

        {globalError && <p className="auth-error">{globalError}</p>}

        <button className="global_btn" type="submit"> Login </button>
        <p className="auth-link"> Don't have an account? <a href="/register">Register here</a> </p>

      </form>
    </div>
  );
}
