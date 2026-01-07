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
        <div className="glass-card" style={{ padding: '16px 24px', marginBottom: 24 }}>
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
                style={{ display: 'flex', flexWrap: 'wrap', gap: '8px', alignItems: 'center' }}
            >
                {config.map(field => (
                    <Form.Item
                        key={field.name}
                        name={field.name}
                        label={<span style={{ fontWeight: 600, color: 'var(--otherColor2)' }}>{field.label}</span>}
                        style={{ marginBottom: 8, marginRight: 16 }}
                    >
                        {field.type === "input" && (
                            <Input placeholder={field.placeholder} allowClear style={{ borderRadius: 6 }} />
                        )}

                        {field.type === "number" && (
                            <Input placeholder={field.placeholder} allowClear style={{ borderRadius: 6 }} />
                        )}

                        {field.type === "select" && (
                            <Select
                                allowClear
                                style={{ minWidth: field.width || 180, borderRadius: 6 }}
                                placeholder={field.placeholder}
                                options={field.options}
                                popupMatchSelectWidth={false}
                            />
                        )}

                        {field.type === "dateRange" && <RangePicker allowClear style={{ borderRadius: 6 }} />}
                    </Form.Item>
                ))}

                {/* Search Button next to filters */}
                <Form.Item style={{ marginBottom: 8 }}>
                    <Button
                        type="primary"
                        htmlType="submit"
                        icon={<SearchOutlined />}
                        style={{ background: 'var(--vibrant-gradient)', border: 'none', borderRadius: 6, fontWeight: 600 }}
                    >
                        Search
                    </Button>
                </Form.Item>
                {onAdd && (
                    <Form.Item style={{ margin: 0 }}>
                        <Button
                            type="primary"
                            icon={<PlusOutlined />}
                            onClick={onAdd}
                            style={{ background: '#1E5631', border: 'none', borderRadius: 6, fontWeight: 600 }}
                        >
                            Add New
                        </Button>
                    </Form.Item>
                )}
            </Form>
        </div>
    );
}