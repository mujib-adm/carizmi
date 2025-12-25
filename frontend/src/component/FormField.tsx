import { ExclamationCircleOutlined } from "@ant-design/icons";
import { Tooltip } from "antd";
import { FieldError } from "react-hook-form";

interface FormFieldProps {
    type?: string; // "text", "email", "password" etc.
    placeholder?: string;
    registerProps: any; // from RHF's register()
    error?: FieldError;
    as?: "input" | "textarea" | "select"; // NEW: choose element type
    options?: { value: string; label: string }[]; // for select dropdowns
}

export function FormField({
    type = "text",
    placeholder,
    registerProps,
    error,
    as = "input",
    options = [],
}: FormFieldProps) {
    return (
        <Tooltip title={error?.message} placement="right" open={!!error}>
            <div style={{ position: "relative", marginBottom: "1rem", display: "flex", alignItems: "center" }}>
                {as === "input" && (
                    <input
                        type={type}
                        placeholder={placeholder}
                        className={`form_field ${error ? "error" : ""}`}
                        {...registerProps}
                        style={{ flex: 1 }}
                    />
                )}

                {as === "textarea" && (
                    <textarea
                        placeholder={placeholder}
                        className={`form_field ${error ? "error" : ""}`}
                        {...registerProps}
                    />
                )}

                {as === "select" && (
                    <select
                        className={`form_field ${error ? "error" : ""}`}
                        {...registerProps}
                    >
                        <option value="">Select an option</option>
                        {options.map((opt) => (
                            <option key={opt.value} value={opt.value}>
                                {opt.label}
                            </option>
                        ))}
                    </select>
                )}

                {error && (
                    <ExclamationCircleOutlined
                        style={{
                            position: "absolute",
                            right: "12px",
                            top: "0",
                            bottom: "0",
                            margin: "auto",
                            height: "100%",
                            display: "flex",
                            alignItems: "center",
                            color: "#d93025",
                            fontSize: "18px",
                            pointerEvents: "none"
                        }}
                    />

                )}
            </div>
        </Tooltip>
    );
};