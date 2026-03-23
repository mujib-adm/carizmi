import {
  LockOutlined,
  SaveOutlined,
  UserOutlined,
  MailOutlined,
  IdcardOutlined,
} from '@ant-design/icons';
import {
  Avatar,
  Button,
  Card,
  Col,
  Descriptions,
  Divider,
  Form,
  Modal,
  Row,
  Typography,
} from 'antd';
import { useEffect, useState } from 'react';
import { authenticationApi } from '../../api/generated/authentication/authentication';
import { AntdFormItem } from '../../component/AntdFormItem';
import { MessageBanner } from '../../component/MessageBanner';
import { useNotification } from '../../context/NotificationContext';
import { useApiMessages } from '../../hook/ApiResponseHandler';
import Sidebar from '../../component/Sidebar';
import '../../themes/modern-ui.css';

import {
  MessageType,
  UserProfileDto,
  PasswordUpdateRequestDto
} from '../../api/generated/types';

const { Title, Text } = Typography;

export default function ProfilePage() {
  const [profileData, setProfileData] = useState<UserProfileDto | null>(null);
  const [passwordForm] = Form.useForm();
  const [loading, setLoading] = useState(true);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const notify = useNotification();

  const { globalMessages, handleResponse, handleError, resetMessages } = useApiMessages(
    undefined,
    (field, msg) => {
      passwordForm.setFields([{ name: field, errors: [msg] }]);
    }
  );

  const fetchProfile = async () => {
    setLoading(true);
    try {
      resetMessages();
      const res = await authenticationApi.getCurrentUser();
      if (res.responseData) {
        setProfileData(res.responseData);
      }
    } catch (err: any) {
      handleError(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchProfile();
  }, []);

  const onUpdatePassword = async (values: PasswordUpdateRequestDto) => {
    try {
      resetMessages();
      const res = await authenticationApi.updatePassword(values);
      if (res.globalMessages && res.globalMessages.length > 0) {
        const msg = res.globalMessages[0];
        if (msg.type === MessageType.SUCCESS) {
          setIsModalOpen(false);
          notify.success({ message: 'Success', description: msg.message }, '/logout');
        } else {
          handleResponse(res as any);
        }
      }
    } catch (err: any) {
      handleError(err);
    }
  };

  return (
    <div className="dashboard-layout">
      <Sidebar />
      <main className="content fade-in">
        <div style={{ padding: 24 }}>
          <div className="page-header">
            <Title level={2} className="page-title">
              <UserOutlined /> User Profile
            </Title>
          </div>

          <Row gutter={[24, 24]} justify="center">
            <Col xs={24} xl={16}>
              <Card
                className="glass-card fade-in"
                style={{ borderRadius: '24px', overflow: 'hidden' }}
                loading={loading}
              >
                <div
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: '32px',
                    marginBottom: '40px',
                    flexWrap: 'wrap',
                  }}
                >
                  <Avatar
                    size={120}
                    icon={<UserOutlined />}
                    style={{
                      backgroundColor: '#40916C',
                      boxShadow: '0 8px 24px rgba(64, 145, 108, 0.2)',
                      flexShrink: 0,
                    }}
                  />
                  <div>
                    <Title level={2} style={{ margin: 0, color: '#1E5631' }}>
                      {profileData?.firstName} {profileData?.lastName}
                    </Title>
                    <Text
                      type="secondary"
                      style={{ fontSize: '1.2rem', display: 'block', marginTop: '4px' }}
                    >
                      {profileData?.role}
                    </Text>
                  </div>
                </div>

                <Divider />

                {globalMessages && <MessageBanner messages={globalMessages} />}

                <Descriptions
                  title="Personal Information"
                  column={{ xxl: 2, xl: 2, lg: 2, md: 1, sm: 1, xs: 1 }}
                  labelStyle={{ fontWeight: 600, color: '#666666' }}
                  contentStyle={{ color: '#1E5631' }}
                >
                  <Descriptions.Item
                    label={
                      <span>
                        <UserOutlined /> First Name
                      </span>
                    }
                  >
                    {profileData?.firstName || '--'}
                  </Descriptions.Item>
                  <Descriptions.Item
                    label={
                      <span>
                        <UserOutlined /> Last Name
                      </span>
                    }
                  >
                    {profileData?.lastName || '--'}
                  </Descriptions.Item>
                  <Descriptions.Item
                    label={
                      <span>
                        <IdcardOutlined /> Username
                      </span>
                    }
                  >
                    {profileData?.username || '--'}
                  </Descriptions.Item>
                  <Descriptions.Item
                    label={
                      <span>
                        <MailOutlined /> Email
                      </span>
                    }
                  >
                    {profileData?.email || '--'}
                  </Descriptions.Item>
                </Descriptions>
                <Divider />
                <Button
                  type="primary"
                  icon={<LockOutlined />}
                  onClick={() => setIsModalOpen(true)}
                  style={{
                    marginTop: '16px',
                    borderRadius: '8px',
                    background: '#40916C',
                    borderColor: '#40916C',
                  }}
                >
                  Change Password
                </Button>
              </Card>
            </Col>
          </Row>
        </div>

        <Modal
          title={
            <Title level={4} style={{ margin: 0 }}>
              <LockOutlined /> Change Password
            </Title>
          }
          open={isModalOpen}
          onCancel={() => {
            setIsModalOpen(false);
            passwordForm.resetFields();
            resetMessages();
          }}
          footer={null}
          centered
          className="modern-modal"
          width={500}
        >
          <Form
            form={passwordForm}
            layout="vertical"
            onFinish={onUpdatePassword}
            requiredMark="optional"
            style={{ marginTop: '24px' }}
          >
            <Divider style={{ margin: '24px 0' }}>
              <Text type="secondary" style={{ fontSize: '12px' }}>
                CURRENT PASSWORD
              </Text>
            </Divider>

            <AntdFormItem
              name="oldPassword"
              label="Current Password"
              type="password"
              rules={[{ required: true, message: 'Current password is required' }]}
              placeholder="Enter current password"
            />

            <Divider style={{ margin: '24px 0' }}>
              <Text type="secondary" style={{ fontSize: '12px' }}>
                NEW PASSWORD
              </Text>
            </Divider>

            <AntdFormItem
              name="newPassword"
              label="New Password"
              type="password"
              rules={[
                { required: true, message: 'New password is required' },
                { min: 5, message: 'Password must be at least 5 characters' },
              ]}
              placeholder="Enter new password"
            />

            <AntdFormItem
              name="confirmPassword"
              label="Confirm New Password"
              type="password"
              dependencies={['newPassword']}
              rules={[
                { required: true, message: 'Please confirm your new password' },
                ({ getFieldValue }) => ({
                  validator(_, value) {
                    if (!value || getFieldValue('newPassword') === value) {
                      return Promise.resolve();
                    }
                    return Promise.reject(new Error('The two passwords do not match!'));
                  },
                }),
              ]}
              placeholder="Confirm new password"
            />

            {globalMessages && <MessageBanner messages={globalMessages} />}

            <div style={{ marginTop: '32px' }}>
              <Button
                type="primary"
                htmlType="submit"
                icon={<SaveOutlined />}
                block
                size="large"
                style={{
                  borderRadius: '12px',
                  height: '48px',
                  background: 'linear-gradient(135deg, #2D6A4F 0%, #1E5631 100%)',
                  border: 'none',
                  boxShadow: '0 4px 12px rgba(30, 86, 49, 0.2)',
                }}
              >
                Update Password
              </Button>
            </div>
          </Form>
        </Modal>
      </main>
    </div>
  );
}
