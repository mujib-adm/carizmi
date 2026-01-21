import { EditOutlined, UserOutlined } from '@ant-design/icons';
import { Button, Card, Col, Divider, Row, Space, Table, Tag, Typography, message } from "antd";
import { useEffect, useState } from "react";
import { getAllUsers, updateUserRole, updateUserStatus } from "../../apiclient/userApi";
import { MessageBanner } from '../../component/MessageBanner';
import Sidebar from "../../component/Sidebar";
import "../../component/Sidebar.css";
import { UserModal } from '../../modals/UserModal';
import { RoleConstants } from "../../constants/RoleConstants";
import { MessageType, User } from "../../constants/types";
import { useApiMessages } from "../../hook/ApiResponseHandler";
import { useNotification } from "../../context/NotificationContext";

const { Title } = Typography;

export default function UsersPage() {
    const [users, setUsers] = useState<User[]>([]);
    const [loading, setLoading] = useState(false);
    const [modalOpen, setModalOpen] = useState(false);
    const [selectedUser, setSelectedUser] = useState<User | null>(null);
    const notify = useNotification();

    const { globalMessages, handleError, resetMessages } = useApiMessages();

    const fetchUsers = async () => {
        setLoading(true);
        resetMessages();
        try {
            const response = await getAllUsers();
            if (response.responseData) {
                setUsers(response.responseData);
            } else {
                setUsers([]);
            }
        } catch (error: any) {
            handleError(error);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchUsers();
    }, []);

    const openEdit = (user: User) => {
        setSelectedUser(user);
        setModalOpen(true);
    };

    const handleUpdateUser = async (updatedValues: any) => {
        try {
            // Check and update Role if changed
            let resp;
            if (updatedValues.role !== selectedUser?.role) {
                resp = await updateUserRole(updatedValues.userID, updatedValues.role);
            }
            // Check and update Status if changed
            if (updatedValues.active !== selectedUser?.active) {
                resp = await updateUserStatus(updatedValues.userID, updatedValues.active);
            }
            if (resp.globalMessages?.[0]?.type === MessageType.SUCCESS) {
                notify.success({ message: "Success", description: resp.globalMessages[0].message });
            }
            setModalOpen(false);
            setSelectedUser(null);
            fetchUsers();
        } catch (error: any) {
            throw error;
        }
    };

    const columns = [
        { title: "Name", key: "name", render: (_: any, record: User) => `${record.firstName} ${record.lastName}`, },
        { title: "Email", dataIndex: "email", key: "email", },
        { title: "Username", dataIndex: "username", key: "username", },
        {
            title: "Role",
            key: "role",
            render: (_: any, record: User) => {
                let color = 'geekblue';
                if (record.role === RoleConstants.ROLE_ADMIN) color = 'volcano';
                if (record.role === RoleConstants.ROLE_MANAGER) color = 'gold';
                return (
                    <Tag color={color}>
                        {record.role.replace("ROLE_", "")}
                    </Tag>
                );
            },
        },
        {
            title: "Status",
            key: "active",
            render: (_: any, record: User) => (
                <Tag color={record.active ? 'green' : 'red'}>
                    {record.active ? 'Active' : 'Inactive'}
                </Tag>
            ),
        },
        {
            title: "Action",
            key: "action",
            render: (_: any, record: User) => (
                <Space>
                    <Button icon={<EditOutlined />} onClick={() => openEdit(record)} />
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
                            <UserOutlined /> User Management
                        </Title>
                    </div>

                    <Row gutter={[24, 24]} justify="center">
                        <Col xs={24} xl={16}>
                            <Card className="glass-card">
                                <div className="chart-title" style={{ marginBottom: '28px' }}>Users List</div>
                                <Divider />

                                {globalMessages && <MessageBanner messages={globalMessages} />}

                                <Table
                                    loading={loading}
                                    dataSource={users}
                                    columns={columns}
                                    rowKey="userID"
                                    pagination={{ pageSize: 10 }}
                                    size="small"
                                />
                            </Card>
                        </Col>
                    </Row>

                    <UserModal
                        open={modalOpen}
                        onCancel={() => setModalOpen(false)}
                        onSubmit={handleUpdateUser}
                        initialValues={selectedUser}
                    />
                </div>
            </main>
        </div>
    );
}