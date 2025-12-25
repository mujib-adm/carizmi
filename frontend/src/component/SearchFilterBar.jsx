import { PlusOutlined, SearchOutlined } from "@ant-design/icons";
import { Button, DatePicker, Form, Input, Select } from "antd";
import dayjs from "dayjs";

const { RangePicker } = DatePicker;

export default function SearchFilterBar({
    config = [],
    filters,
    onChange,
    onSearch,
    onAdd
}) {
    const [form] = Form.useForm();

    // Convert filters → initial form values
    const initialValues = {};
    config.forEach(field => {
        if (field.type === "dateRange") {
            if (filters.dateFrom && filters.dateTo) {
                initialValues[field.name] = [
                    dayjs(filters.dateFrom),
                    dayjs(filters.dateTo)
                ];
            }
        } else {
            initialValues[field.name] = filters[field.name];
        }
    });

    return (
        <Form
            form={form}
            layout="inline"
            initialValues={initialValues}
            onValuesChange={(changed, allValues) => {
                const updated = { ...filters };

                config.forEach(field => {
                    const value = allValues[field.name];

                    if (field.type === "dateRange") {
                        updated.dateFrom = value?.[0]
                            ? value[0].startOf("day").format("YYYY-MM-DD")
                            : undefined;

                        updated.dateTo = value?.[1]
                            ? value[1].endOf("day").format("YYYY-MM-DD")
                            : undefined;
                    } else if (field.type === "number") {
                        updated[field.name] =
                            value && !Number.isNaN(Number(value))
                                ? Number(value)
                                : undefined;
                    } else {
                        updated[field.name] = value || undefined;
                    }
                });

                onChange(updated);
            }}
            onFinish={onSearch}
        >
            {config.map(field => (
                <Form.Item
                    key={field.name}
                    name={field.name}
                    label={field.label}
                    style={{ marginBottom: 8 }}
                >
                    {field.type === "input" && (
                        <Input placeholder={field.placeholder} allowClear />
                    )}

                    {field.type === "number" && (
                        <Input placeholder={field.placeholder} allowClear />
                    )}

                    {field.type === "select" && (
                        <Select
                            allowClear
                            style={{ minWidth: field.width || 150 }}
                            placeholder={field.placeholder}
                            options={field.options}
                        />
                    )}

                    {field.type === "dateRange" && <RangePicker allowClear />}
                </Form.Item>
            ))}

            {/* Search Button */}
            <Form.Item>
                <Button type="primary" htmlType="submit" icon={<SearchOutlined />}>Search</Button>
            </Form.Item>
            {/* Add New Button */}
            {onAdd && (
                <Form.Item>
                    <Button type="primary" icon={<PlusOutlined />} onClick={onAdd} style={{ marginLeft: 16 }} >Add New</Button>
                </Form.Item>
            )}
        </Form>
    );
}