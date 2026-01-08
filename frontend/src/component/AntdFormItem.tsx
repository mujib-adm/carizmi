import { DatePicker, Form, FormItemProps, Input, Select } from 'antd';
import { Rule } from 'antd/es/form';
import React from 'react';

const { Option } = Select;

interface AntdFormItemProps extends FormItemProps {
    name: string;
    label?: string;
    type?: 'text' | 'number' | 'select' | 'date' | 'tel' | 'email';
    options?: { value: string | number; label: string | number }[];
    placeholder?: string;
    rules?: Rule[];
    initialValue?: any;
    children?: React.ReactNode;
    disabled?: boolean;
    inputProps?: any; // Pass props to the underlying input
}

export function AntdFormItem({
    name,
    label,
    type = 'text',
    options = [],
    placeholder,
    rules,
    initialValue,
    children,
    disabled = false,
    inputProps,
    ...rest
}: AntdFormItemProps) {

    const mergedInputProps = { ...inputProps, disabled };

    const renderInput = () => {
        if (children) return children;

        switch (type) {
            case 'select':
                return (
                    <Select placeholder={placeholder} allowClear {...mergedInputProps}>
                        {options.map(opt => (
                            <Option key={opt.value} value={opt.value}>
                                {opt.label}
                            </Option>
                        ))}
                    </Select>
                );
            case 'date':
                return (
                    <DatePicker
                        style={{ width: '100%' }}
                        placeholder={placeholder}
                        format="MM/DD/YYYY"
                        {...mergedInputProps}
                    />
                );
            case 'number':
                return <Input type="number" placeholder={placeholder} {...mergedInputProps} />;
            case 'tel':
                return <Input type="tel" placeholder={placeholder} {...mergedInputProps} />;
            case 'email':
                return <Input type="email" placeholder={placeholder} {...mergedInputProps} />;
            case 'text':
            default:
                return <Input placeholder={placeholder} {...mergedInputProps} />;
        }
    };

    return (
        <Form.Item
            name={name}
            label={label}
            rules={rules}
            initialValue={initialValue}
            {...rest}
        >
            {renderInput()}
        </Form.Item>
    );
};