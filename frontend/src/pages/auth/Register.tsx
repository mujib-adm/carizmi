import React from "react";
import { Alert } from "antd";
import { Navigate, useNavigate } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import "../../themes/css/auth-form.css";
import { useForm } from "react-hook-form";

import apiClient from "../../context/ApiClient";
import { normalizeAxiosResponse } from "../../context/ErrorUtils";
import { GlobalResponse, MessageType, RegisterForm } from "../../constants/types";
import { FormField } from "../FormField";
import { useNotification } from "../../context/NotificationContext";

export default function Register() {
  const navigate = useNavigate();
  const { token, isLoading } = useAuth();
  const notify = useNotification();

  const {
    register,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<RegisterForm>();

  const [globalError, setGlobalError] = React.useState("");

  const onSubmit = async (formValues: RegisterForm) => {
    setGlobalError("");

    try {
      const response = await apiClient.post<GlobalResponse>("/auth/register", formValues);
      const body = response.data;

      if (body.globalMessages?.length > 0) {
        const msg = body.globalMessages[0];
        if (msg.type === MessageType.WARNING) {
          notify.warning({ message: "Warning", description: msg.message }, "/login");
        } else {
          notify.success({ message: "Success", description: msg.message }, "/login");
        }
      }


    } catch (error: any) {
      // AxiosError<GlobalResponse>
      const backendResponse: GlobalResponse | undefined = error.response?.data;
      if (backendResponse) {
        const normalized = normalizeAxiosResponse(backendResponse);

        // Map fieldMessages → RHF errors
        Object.entries(normalized.fieldMessages).forEach(([field, message]) => {
          setError(field as keyof RegisterForm, { type: "server", message, });
        });

        // Global errors → banner
        if (normalized.globalMessages.length > 0) {
          setGlobalError(normalized.globalMessages[0]);
        } else {
          setGlobalError(error.message || "Registration failed. Please try again or contact support.");
        }
      }

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

        {/* {globalError && (<Alert message="Registration Error" description={globalError} type="error" showIcon closable style={{ marginBottom: "16px" }} />)} */}

        <button className="global_btn" type="submit"> Register </button>
        <p className="auth-link"> Already have an account? <a href="/login">Login here</a> </p>
      </form>

    </div>
  );
}