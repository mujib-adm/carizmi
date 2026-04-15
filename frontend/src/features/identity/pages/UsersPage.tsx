import { EditOutlined, PlusOutlined, UserOutlined } from '@ant-design/icons';
import { Button, Card, Divider, Space, Table, Tag, Typography } from 'antd';
import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { userManagementApi } from '../../../api/generated/user-management/user-management';
import { MessageBanner } from '../../../components/MessageBanner';
import { UserModal } from '../modals/UserModal';
import { RoleConstants } from '../../../constants/RoleConstants';
import {
  MessageType,
  UserResponseDto,
  UserStatusUpdateRequestDto,
} from '../../../api/generated/types';
import { useApiMessages } from '../../../hooks/useApiMessages';
import { useNotification } from '../../../hooks/useNotification';

const { Title } = Typography;

export default function UsersPage() {
  const navigate = useNavigate();
  const [users, setUsers] = useState<UserResponseDto[]>([]);
  const [loading, setLoading] = useState(false);
  const [modalOpen, setModalOpen] = useState(false);
  const [selectedUser, setSelectedUser] = useState<UserResponseDto | null>(null);
  const notify = useNotification();

  const { globalMessages, handleError, resetMessages } = useApiMessages();

  const fetchUsers = async () => {
    setLoading(true);
    resetMessages();
    try {
      const response = await userManagementApi.getAllUsers();
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

  const openEdit = (user: UserResponseDto) => {
    setSelectedUser(user);
    setModalOpen(true);
  };

  const handleUpdateUser = async (updatedValues: any) => {
    if (!selectedUser?.userID) return;
    // Check and update Role if changed
    let resp;
    if (updatedValues.role !== selectedUser?.role) {
      resp = await userManagementApi.updateRole(selectedUser.userID, {
        role: updatedValues.role,
      });
    }
    // Check and update Status if changed
    if (updatedValues.active !== selectedUser?.active) {
      const statusReq: UserStatusUpdateRequestDto = { active: updatedValues.active };
      resp = await userManagementApi.toggleStatus(selectedUser.userID, statusReq);
    }
    if (resp?.globalMessages?.[0]?.type === MessageType.SUCCESS) {
      notify.success({ message: 'Success', description: resp.globalMessages[0].message });
    }
    setModalOpen(false);
    setSelectedUser(null);
    fetchUsers();
  };

  const columns = [
    {
      title: 'Name',
      key: 'name',
      render: (_: any, record: UserResponseDto) => `${record.firstName} ${record.lastName}`,
    },
    { title: 'Email', dataIndex: 'email', key: 'email' },
    { title: 'Username', dataIndex: 'username', key: 'username' },
    {
      title: 'Role',
      key: 'role',
      render: (_: any, record: UserResponseDto) => {
        let color = 'geekblue';
        if (record.role === RoleConstants.ROLE_ADMIN) color = 'volcano';
        if (record.role === RoleConstants.ROLE_MANAGER) color = 'gold';
        return <Tag color={color}>{(record.role || '').replace('ROLE_', '')}</Tag>;
      },
    },
    {
      title: 'Status',
      key: 'active',
      render: (_: any, record: UserResponseDto) => (
        <Tag color={record.active ? 'green' : 'red'}>{record.active ? 'Active' : 'Inactive'}</Tag>
      ),
    },
    {
      title: 'Action',
      key: 'action',
      render: (_: any, record: UserResponseDto) => (
        <Space>
          <Button icon={<EditOutlined />} onClick={() => openEdit(record)} />
        </Space>
      ),
    },
  ];

  return (
    <div>
      <div className="page-header">
        <Title level={2} className="page-title">
          <UserOutlined /> User Management
        </Title>
      </div>

      <Card className="glass-card">
        <div className="chart-title">Users List</div>
        <Divider />

        {globalMessages && <MessageBanner messages={globalMessages} />}

        <Table
          scroll={{ x: 'max-content' }}
          loading={loading}
          dataSource={users}
          columns={columns}
          rowKey="userID"
          pagination={{ pageSize: 10 }}
          size="small"
        />

        <div style={{ marginTop: 12 }}>
          <Button
            type="default"
            ghost
            icon={<PlusOutlined />}
            onClick={() => navigate('/register')}
            className="outline-action-btn"
          >
            Register New User
          </Button>
        </div>
      </Card>

      <UserModal
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        onSubmit={handleUpdateUser}
        initialValues={selectedUser}
      />
    </div>
  );
}