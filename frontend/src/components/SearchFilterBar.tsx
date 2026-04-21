import { FilterOutlined, PlusOutlined, SearchOutlined } from '@ant-design/icons';
import { Button, DatePicker, Form, Grid, Input, Select } from 'antd';
import { useState } from 'react';
import dayjs from 'dayjs';

const { RangePicker } = DatePicker;
const { useBreakpoint } = Grid;

type FilterFieldConfig = {
  name: string;
  label: string;
  type: 'input' | 'number' | 'select' | 'dateRange';
  placeholder?: string;
  width?: number;
  options?: { value: string; label: string }[];
};

type SearchFilterBarProps = {
  config?: FilterFieldConfig[];
  filters: Record<string, any>;
  onChange: (updated: Record<string, any>) => void;
  onSearch: () => void;
  onAdd?: () => void;
};

export default function SearchFilterBar({
  config = [],
  filters,
  onChange,
  onSearch,
  onAdd,
}: SearchFilterBarProps) {
  const [form] = Form.useForm();
  const screens = useBreakpoint();
  const isMobile = !screens.md; // true for < 768px
  const [filtersVisible, setFiltersVisible] = useState(false);

  // Convert filters → initial form values
  const initialValues: Record<string, any> = {};
  config.forEach((field) => {
    if (field.type === 'dateRange') {
      if (filters.dateFrom && filters.dateTo) {
        initialValues[field.name] = [dayjs(filters.dateFrom), dayjs(filters.dateTo)];
      }
    } else {
      initialValues[field.name] = filters[field.name];
    }
  });

  const handleValuesChange = (_changed: any, allValues: any) => {
    const updated: Record<string, any> = { ...filters };

    config.forEach((field) => {
      const value = allValues[field.name];

      if (field.type === 'dateRange') {
        updated.dateFrom = value?.[0]
          ? value[0].startOf('day').format('YYYY-MM-DD')
          : undefined;

        updated.dateTo = value?.[1] ? value[1].endOf('day').format('YYYY-MM-DD') : undefined;
      } else if (field.type === 'number') {
        updated[field.name] =
          value && !Number.isNaN(Number(value)) ? Number(value) : undefined;
      } else {
        updated[field.name] = value || undefined;
      }
    });

    onChange(updated);
  };

  const handleSearch = () => {
    onSearch();
    if (isMobile) setFiltersVisible(false);
  };

  // ── Mobile layout: collapsible filter drawer ──
  if (isMobile) {
    return (
      <div className="glass-card" style={{ padding: '12px', marginBottom: 16 }}>
        {/* Always-visible: action buttons row */}
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
          <Button
            icon={<FilterOutlined />}
            onClick={() => setFiltersVisible((v) => !v)}
            style={{
              flex: '1 1 0',
              minWidth: 0,
              borderRadius: 6,
              fontWeight: 600,
              borderColor: filtersVisible ? 'var(--priColor)' : undefined,
              color: filtersVisible ? 'var(--priColor)' : undefined,
            }}
          >
            {filtersVisible ? 'Hide' : 'Filters'}
          </Button>
          <Button
            type="primary"
            icon={<SearchOutlined />}
            onClick={handleSearch}
            style={{
              flex: '1 1 0',
              minWidth: 0,
              background: 'var(--vibrant-gradient)',
              border: 'none',
              borderRadius: 6,
              fontWeight: 600,
            }}
          >
            Search
          </Button>
          {onAdd && (
            <Button
              type="primary"
              icon={<PlusOutlined />}
              onClick={onAdd}
              className="action-btn-primary"
              style={{ flex: '1 1 0', minWidth: 0 }}
            >
              Add New
            </Button>
          )}
        </div>

        {/* Collapsible filter fields */}
        <div
          style={{
            maxHeight: filtersVisible ? 500 : 0,
            overflow: 'hidden',
            transition: 'max-height 0.3s ease-in-out',
            marginTop: filtersVisible ? 12 : 0,
          }}
        >
          <Form
            form={form}
            layout="vertical"
            initialValues={initialValues}
            onValuesChange={handleValuesChange}
            onFinish={handleSearch}
          >
            {config.map((field) => (
              <Form.Item
                key={field.name}
                name={field.name}
                label={
                  <span style={{ fontWeight: 600, color: 'var(--otherColor2)' }}>
                    {field.label}
                  </span>
                }
                style={{ marginBottom: 10 }}
              >
                {field.type === 'input' && (
                  <Input
                    placeholder={field.placeholder}
                    allowClear
                    style={{ borderRadius: 6, width: '100%' }}
                  />
                )}

                {field.type === 'number' && (
                  <Input
                    placeholder={field.placeholder}
                    allowClear
                    style={{ borderRadius: 6, width: '100%' }}
                  />
                )}

                {field.type === 'select' && (
                  <Select
                    allowClear
                    style={{ width: '100%', borderRadius: 6 }}
                    placeholder={field.placeholder}
                    options={field.options}
                    popupMatchSelectWidth={false}
                  />
                )}

                {field.type === 'dateRange' && (
                  <RangePicker
                    allowClear
                    style={{ borderRadius: 6, width: '100%' }}
                  />
                )}
              </Form.Item>
            ))}
          </Form>
        </div>
      </div>
    );
  }

  // ── Desktop layout: inline (original behavior) ──
  return (
    <div className="glass-card" style={{ padding: '16px 24px', marginBottom: 24 }}>
      <Form
        form={form}
        layout="inline"
        initialValues={initialValues}
        onValuesChange={handleValuesChange}
        onFinish={onSearch}
        style={{ display: 'flex', flexWrap: 'wrap', gap: '8px', alignItems: 'center' }}
      >
        {config.map((field) => (
          <Form.Item
            key={field.name}
            name={field.name}
            label={
              <span style={{ fontWeight: 600, color: 'var(--otherColor2)' }}>{field.label}</span>
            }
            style={{ marginBottom: 8, marginRight: 16 }}
          >
            {field.type === 'input' && (
              <Input placeholder={field.placeholder} allowClear style={{ borderRadius: 6 }} />
            )}

            {field.type === 'number' && (
              <Input placeholder={field.placeholder} allowClear style={{ borderRadius: 6 }} />
            )}

            {field.type === 'select' && (
              <Select
                allowClear
                style={{ minWidth: field.width || 180, borderRadius: 6 }}
                placeholder={field.placeholder}
                options={field.options}
                popupMatchSelectWidth={false}
              />
            )}

            {field.type === 'dateRange' && <RangePicker allowClear style={{ borderRadius: 6 }} />}
          </Form.Item>
        ))}

        {/* Search Button next to filters */}
        <Form.Item style={{ marginBottom: 8 }}>
          <Button
            type="primary"
            htmlType="submit"
            icon={<SearchOutlined />}
            style={{
              background: 'var(--vibrant-gradient)',
              border: 'none',
              borderRadius: 6,
              fontWeight: 600,
            }}
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
              className="action-btn-primary"
            >
              Add New
            </Button>
          </Form.Item>
        )}
      </Form>
    </div>
  );
}