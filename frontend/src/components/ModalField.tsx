import { ExclamationCircleOutlined } from '@ant-design/icons';
import { DatePicker, Select, Tooltip } from 'antd';
import dayjs from 'dayjs';
import React from 'react';
import { Control, Controller, FieldError } from 'react-hook-form';

interface FormFieldProps {
  // common
  name: string; // required: form field name
  label?: string;
  placeholder?: string;
  error?: FieldError;
  // input/textarea
  type?: string; // "text", "email", "password" etc.
  registerProps?: any; // from RHF's register("fieldName")
  // which element to render
  as?: 'input' | 'textarea' | 'select' | 'date';
  // select-only
  control?: Control<any>; // from useForm()
  options?: { value: string; label: string }[];
  allowClear?: boolean;
  disabled?: boolean;
  style?: React.CSSProperties;
}

export function ModalField({
  name,
  label,
  placeholder,
  error,
  type = 'text',
  registerProps,
  as = 'input',
  control,
  options = [],
  allowClear = true,
  disabled = false,
  style,
}: FormFieldProps) {
  const showError = !!error;
  const tooltipTitle = error?.message;

  return (
    <Tooltip title={tooltipTitle} placement="right" open={showError}>
      <div style={{ position: 'relative', marginBottom: '1rem' }}>
        {label && (
          <label htmlFor={name} style={{ display: 'block' }}>
            {label}
          </label>
        )}

        {as === 'input' && (
          <input
            id={name}
            type={type}
            placeholder={placeholder}
            className={`form_field ${showError ? 'error' : ''}`}
            {...registerProps}
            style={{ width: '100%', ...(style || {}) }}
            disabled={disabled}
          />
        )}

        {as === 'textarea' && (
          <textarea
            id={name}
            placeholder={placeholder}
            className={`form_field ${showError ? 'error' : ''}`}
            {...registerProps}
            style={{ width: '100%', minHeight: 96, ...(style || {}) }}
            disabled={disabled}
          />
        )}

        {as === 'select' && control && (
          <Controller
            control={control}
            name={name}
            render={({ field }) => (
              <Select
                {...field}
                placeholder={placeholder || 'Select an option'}
                options={options}
                allowClear={allowClear}
                disabled={disabled}
                style={{ width: '100%', ...(style || {}) }}
                value={field.value}
                onChange={(val) => field.onChange(val)}
                onBlur={field.onBlur}
              />
            )}
          />
        )}

        {as === 'date' && control && (
          <Controller
            control={control}
            name={name}
            render={({ field }) => (
              <DatePicker
                {...field}
                format="MM/DD/YYYY"
                placeholder={placeholder || 'Select date'}
                style={{ width: '100%', ...(style || {}) }}
                disabled={disabled}
                value={field.value ? dayjs(field.value) : null}
                onChange={(date) => field.onChange(date ? date.toISOString() : null)}
              />
            )}
          />
        )}

        {showError && (
          <ExclamationCircleOutlined
            style={{
              position: 'absolute',
              right: 12,
              top: '70%',
              transform: 'translateY(-50%)',
              color: 'var(--errorColor)',
              fontSize: 18,
              pointerEvents: 'none',
            }}
          />
        )}
      </div>
    </Tooltip>
  );
}
