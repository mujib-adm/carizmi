import { DeleteOutlined, EditOutlined, PlusOutlined, SearchOutlined } from '@ant-design/icons';
import { Button, Card, Form, Input, message, Modal, Select, Space, Table, Tag, Typography } from 'antd';
import { useEffect, useState } from 'react';
import { addSystemSetting, deleteSystemSetting, searchSystemSettings, updateSystemSetting } from '../../apiclient/systemSettingsApi';
import Sidebar from "../../component/Sidebar";
import "../../component/Sidebar.css"; // Ensure sidebar styles are loaded

const { Title } = Typography;
const { Option } = Select;

export default function SystemSettingsPage() {
    const [settings, setSettings] = useState([]);
    const [loading, setLoading] = useState(false);
    const [searchParams, setSearchParams] = useState({});
    const [pagination, setPagination] = useState({ current: 1, pageSize: 10, total: 0 });

    // Modal state
    const [modalVisible, setModalVisible] = useState(false);
    const [editingId, setEditingId] = useState<number | null>(null);
    const [form] = Form.useForm();

    useEffect(() => {
        fetchSettings();
    }, []);

    const fetchSettings = async (params: any = {}, page = 1, size = 10) => {
        setLoading(true);
        try {
            const res = await searchSystemSettings({ ...params, page: page - 1, size });
            setSettings(res.data.data || []);
            setPagination({
                current: page,
                pageSize: size,
                total: res.data.meta?.totalRecords || 0
            });
            setSearchParams(params);
        } catch (e) {
            message.error("Failed to load settings");
        } finally {
            setLoading(false);
        }
    };

    const handleSearch = (vals: any) => {
        fetchSettings(vals, 1, pagination.pageSize);
    };

    const handleTableChange = (newPagination: any) => {
        fetchSettings(searchParams, newPagination.current, newPagination.pageSize);
    };

    const handleAdd = () => {
        setEditingId(null);
        form.resetFields();
        form.setFieldsValue({ active: true });
        setModalVisible(true);
    };

    const handleEdit = (record: any) => {
        setEditingId(record.systemSettingsID);
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
                    fetchSettings(searchParams, pagination.current, pagination.pageSize);
                } catch (e) {
                    message.error("Delete failed");
                }
            }
        });
    };

    const handleSave = async () => {
        try {
            const values = await form.validateFields();
            if (editingId) {
                await updateSystemSetting({ ...values, systemSettingsID: editingId });
                message.success("Updated successfully");
            } else {
                await addSystemSetting(values);
                message.success("Added successfully");
            }
            setModalVisible(false);
            fetchSettings(searchParams, pagination.current, pagination.pageSize);
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
        { title: 'Effective Date', dataIndex: 'effectiveDate', key: 'effectiveDate' },
        {
            title: 'Active',
            dataIndex: 'active',
            key: 'active',
            render: (active: boolean) => active ? <Tag color="green">Active</Tag> : <Tag color="red">Inactive</Tag>
        },
        {
            title: 'Action',
            key: 'action',
            render: (_: any, record: any) => (
                <Space>
                    <Button icon={<EditOutlined />} onClick={() => handleEdit(record)} />
                    <Button icon={<DeleteOutlined />} danger onClick={() => handleDelete(record.systemSettingsID)} />
                </Space>
            )
        }
    ];

    return (
        <div className="dashboard">
            <Sidebar />
            <main className="content">
                <div style={{ padding: 24 }}>
                    <div className="settings-header">
                        <Title level={3}>System Settings</Title>
                    </div>

                    <Card style={{ marginBottom: 16 }}>
                        <Form layout="inline" onFinish={handleSearch}>
                            <Form.Item name="settingType" label="Type">
                                <Input placeholder="Type" />
                            </Form.Item>
                            <Form.Item name="settingKey" label="Key">
                                <Input placeholder="Key" />
                            </Form.Item>
                            <Form.Item>
                                <Space>
                                    <Button type="primary" htmlType="submit" icon={<SearchOutlined />}>Search</Button>
                                    <Button onClick={() => { form.resetFields(); handleSearch({}); }}>Clear</Button>
                                </Space>
                            </Form.Item>
                            <Form.Item>
                                <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd} style={{ marginLeft: 16 }}>Add New</Button>
                            </Form.Item>
                        </Form>
                    </Card>

                    <Table
                        dataSource={settings}
                        columns={columns}
                        rowKey="systemSettingsID"
                        pagination={pagination}
                        loading={loading}
                        onChange={handleTableChange}
                    />

                    <Modal
                        title={editingId ? "Edit Setting" : "Add Setting"}
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
                            <Form.Item name="effectiveDate" label="Effective Date">
                                <Input type="date" />
                            </Form.Item>
                            <Form.Item name="active" label="Active" valuePropName="checked">
                                <Input type="checkbox" />
                            </Form.Item>
                        </Form>
                    </Modal>
                </div>
            </main>
        </div>
    );
}