import { DeleteOutlined, EditOutlined, SettingOutlined } from '@ant-design/icons';
import { Button, Card, Form, Input, message, Modal, Space, Table, Tag, Typography } from 'antd';
import { useEffect, useState } from 'react';
import { addSystemSetting, deleteSystemSetting, updateSystemSetting } from '../../apiclient/systemSettingsApi';
import SearchFilterBar from '../../component/SearchFilterBar.jsx';
import Sidebar from "../../component/Sidebar";
import "../../component/Sidebar.css";
import { systemSettingsSearchFiltersConfig } from '../../constants/systemSettingsSearchFiltersConfig';
import { SystemSetting, SystemSettingSearchParams } from '../../constants/types';
import { useApiMessages } from "../../hook/ApiResponseHandler";
import { usePaginatedSystemSettings } from '../../hook/PaginatedSystemSettings';
import { MessageBanner } from '../../component/MessageBanner.js';

const { Title } = Typography;

export default function SystemSettingsPage() {
    const { settings, meta, loading, fetchSettings } = usePaginatedSystemSettings();
    const [filters, setFilters] = useState<SystemSettingSearchParams>({});
    const { globalMessages, handleError, resetMessages } = useApiMessages<any>();

    // Modal state
    const [modalVisible, setModalVisible] = useState(false);
    const [editingRecord, setEditingRecord] = useState<SystemSetting | null>(null);
    const [form] = Form.useForm();

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

    const handleAdd = () => {
        setEditingRecord(null);
        form.resetFields();
        form.setFieldsValue({ active: true });
        setModalVisible(true);
    };

    const handleEdit = (record: SystemSetting) => {
        setEditingRecord(record);
        form.setFieldsValue(record);
        setModalVisible(true);
    };

    const handleDelete = (id: number) => {
        Modal.confirm({
            title: "Are you sure?",
            content: "This action cannot be undone.",
            onOk: async () => {
                try {
                    await deleteSystemSetting(id);
                    message.success("Deleted successfully");
                    fetchSettings(filters);
                } catch (e) {
                    message.error("Delete failed");
                }
            }
        });
    };

    const handleSave = async () => {
        try {
            const values = await form.validateFields();
            if (editingRecord?.systemSettingsID) {
                await updateSystemSetting({ ...values, systemSettingsID: editingRecord.systemSettingsID });
                message.success("Updated successfully");
            } else {
                await addSystemSetting(values);
                message.success("Added successfully");
            }
            setModalVisible(false);
            fetchSettings(filters);
        } catch (e) {
            console.error(e);
            message.error("Save failed");
        }
    };

    const columns = [
        { title: 'ID', dataIndex: 'systemSettingsID', key: 'systemSettingsID', width: 80 },
        { title: 'Type', dataIndex: 'settingType', key: 'settingType', sorter: true },
        { title: 'Key', dataIndex: 'settingKey', key: 'settingKey', sorter: true },
        { title: 'Value', dataIndex: 'settingValue', key: 'settingValue' },
        {
            title: 'Action',
            key: 'action',
            render: (_: any, record: SystemSetting) => (
                <Space>
                    <Button icon={<EditOutlined />} onClick={() => handleEdit(record)} />
                    <Button icon={<DeleteOutlined />} danger onClick={() => handleDelete(record.systemSettingsID)} />
                </Space>
            )
        }
    ];

    return (
        <div className="dashboard-layout">
            <Sidebar />
            <main className="content fade-in">
                <div style={{ padding: 24 }}>
                    <div className="page-header">
                        <Title level={2} className="page-title">
                            <SettingOutlined /> System Settings
                        </Title>
                    </div>

                    <SearchFilterBar config={systemSettingsSearchFiltersConfig as any} filters={filters} onChange={setFilters as any} onSearch={handleSearch} onAdd={handleAdd} />

                    {globalMessages && <MessageBanner messages={globalMessages} />}

                    <Card className="glass-card" style={{ padding: 0 }}>
                        <Table<SystemSetting>
                            dataSource={settings}
                            columns={columns}
                            rowKey="systemSettingsID"
                            loading={loading}
                            size="small"
                            pagination={{
                                current: meta ? meta.page + 1 : 1,
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
                                    sortOrder: sortOrder === "ascend" ? "asc" : sortOrder === "descend" ? "desc" : undefined,
                                }).catch(handleError);
                            }}
                        />
                    </Card>

                    <Modal
                        title={editingRecord ? "Edit Setting" : "Add Setting"}
                        open={modalVisible}
                        onOk={handleSave}
                        onCancel={() => setModalVisible(false)}
                    >
                        <Form form={form} layout="vertical">
                            <Form.Item name="settingType" label="Type" rules={[{ required: true }]}>
                                <Input />
                            </Form.Item>
                            <Form.Item name="settingKey" label="Key" rules={[{ required: true }]}>
                                <Input />
                            </Form.Item>
                            <Form.Item name="settingValue" label="Value" rules={[{ required: true }]}>
                                <Input />
                            </Form.Item>
                        </Form>
                    </Modal>
                </div>
            </main>
        </div>
    );
}