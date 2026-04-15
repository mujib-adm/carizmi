import { EditOutlined, SettingOutlined } from '@ant-design/icons';
import { Button, Card, Space, Table, Typography } from 'antd';
import { useEffect, useState } from 'react';
import { systemSettingsApi } from '../../../api/generated/system-settings/system-settings';
import { MessageBanner } from '../../../components/MessageBanner';
import SearchFilterBar from '../../../components/SearchFilterBar';
import { SystemSettingsModal } from '../modals/SystemSettingsModal';
import { ReferenceConstants } from '../../../constants/ReferenceConstants';
import { systemSettingsSearchFiltersConfig } from '../config/systemSettingsSearchFiltersConfig';
import { SystemSettingsDto, SystemSettingsSearchRequestDto } from '../../../api/generated/types';
import { useReference } from '../../../hooks/useReference';
import { useApiMessages } from '../../../hooks/useApiMessages';
import { usePaginatedSystemSettings } from '../hooks/usePaginatedSystemSettings';
import { useAuthorization } from '../../../hooks/useAuthorization';

const { Title } = Typography;

export default function SystemSettingsPage() {
  const { settings, meta, loading, fetchSettings } = usePaginatedSystemSettings();
  const [filters, setFilters] = useState<SystemSettingsSearchRequestDto>({});
  const { globalMessages, handleError, resetMessages } = useApiMessages<any>();

  const { canWrite } = useAuthorization();

  const [modalVisible, setModalVisible] = useState(false);
  const [editingRecord, setEditingRecord] = useState<SystemSettingsDto | null>(null);

  // Initial load
  useEffect(() => {
    fetchSettings().catch(handleError);
  }, [fetchSettings, handleError]);

  const handleSearch = async () => {
    resetMessages();
    try {
      await fetchSettings({ ...filters, page: 0, size: meta?.pageSize ?? 10 });
    } catch (e: any) {
      handleError(e);
    }
  };

  const handleEdit = (record: SystemSettingsDto) => {
    setEditingRecord(record);
    setModalVisible(true);
  };

  const handleSave = async (values: SystemSettingsDto) => {
    await systemSettingsApi.updateSystemSetting(values);
    fetchSettings(filters);
  };

  // Helper to format codes like BASELINE_REVENUE -> Baseline Revenue
  const formatCode = (str?: string) => {
    if (!str) return str || '';
    return str
      .replace(/_/g, ' ')
      .toLowerCase()
      .replace(/\b\w/g, (c) => c.toUpperCase());
  };

  const { toDisplay } = useReference();

  const columns = [
    {
      title: 'Setting Name',
      dataIndex: 'settingName',
      key: 'settingName',
      sorter: true,
      render: (text: string) => formatCode(text),
    },
    {
      title: 'Key',
      dataIndex: 'settingKey',
      key: 'settingKey',
      sorter: true,
      render: (text: string, record: SystemSettingsDto) => {
        // If it's a FEE setting, try to use the feeType reference display
        if (record.settingName === 'FEE') {
          const display = toDisplay(ReferenceConstants.FEE_TYPE.NAME, text);
          // toDisplay returns code if not found, check if it's different or just format
          return display !== text ? display : formatCode(text);
        }
        return formatCode(text);
      },
    },
    { title: 'Value', dataIndex: 'settingValue', key: 'settingValue' },
    ...(canWrite
      ? [
          {
            title: 'Action',
            key: 'action',
            render: (_: any, record: SystemSettingsDto) => (
              <Space>
                <Button icon={<EditOutlined />} onClick={() => handleEdit(record)} />
              </Space>
            ),
          },
        ]
      : []),
  ];

  return (
    <div>
      <div className="page-header">
        <Title level={2} className="page-title">
          <SettingOutlined /> System Settings
        </Title>
      </div>

      <SearchFilterBar
        config={systemSettingsSearchFiltersConfig as any}
        filters={filters}
        onChange={setFilters as any}
        onSearch={handleSearch}
        onAdd={undefined}
      />

      {globalMessages && <MessageBanner messages={globalMessages} />}

      <Card className="glass-card" style={{ padding: 0 }}>
        <Table<SystemSettingsDto>
          scroll={{ x: 'max-content' }}
          dataSource={settings}
          columns={columns}
          rowKey="systemSettingsID"
          loading={loading}
          size="small"
          pagination={{
            current: (meta?.page ?? 0) + 1,
            pageSize: meta?.pageSize ?? 10,
            total: meta?.totalRecords ?? 0,
            showSizeChanger: true,
          }}
          onChange={(pagination, _filters, sorter) => {
            const sortField = Array.isArray(sorter) ? sorter[0].field : sorter.field;
            const sortOrder = Array.isArray(sorter) ? sorter[0].order : sorter.order;

            resetMessages();
            fetchSettings({
              ...filters,
              page: (pagination.current ?? 1) - 1,
              size: pagination.pageSize ?? 10,
              sortField: sortField as string,
              sortOrder:
                sortOrder === 'ascend' ? 'asc' : sortOrder === 'descend' ? 'desc' : undefined,
            }).catch(handleError);
          }}
        />
      </Card>

      <SystemSettingsModal
        open={modalVisible}
        onCancel={() => setModalVisible(false)}
        onSubmit={handleSave}
        initial={editingRecord}
      />
    </div>
  );
}