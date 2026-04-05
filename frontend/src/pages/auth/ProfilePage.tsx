import { LockOutlined, UserOutlined, MailOutlined, IdcardOutlined } from '@ant-design/icons';
import { Avatar, Button, Card, Col, Descriptions, Divider, Row, Typography } from 'antd';
import { useEffect, useState } from 'react';
import { authenticationApi } from '../../api/generated/authentication/authentication';
import { MessageBanner } from '../../components/MessageBanner';
import { useNotification } from '../../hooks/useNotification';
import { useApiMessages } from '../../hooks/useApiMessages';
import ChangePasswordModal from '../../modals/ChangePasswordModal';

import { MessageType, UserProfileDto, PasswordUpdateRequestDto } from '../../api/generated/types';
import styles from '../../styles/pages/ProfilePage.module.css';

const { Title, Text } = Typography;

export default function ProfilePage() {
  const [profileData, setProfileData] = useState<UserProfileDto | null>(null);
  const [loading, setLoading] = useState(true);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const notify = useNotification();

  const { globalMessages, handleResponse, handleError, resetMessages } = useApiMessages();

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

  const handleSubmit = async (values: PasswordUpdateRequestDto) => {
    const res = await authenticationApi.updatePassword(values);
    if (res.globalMessages?.[0]?.type === MessageType.SUCCESS) {
      setIsModalOpen(false);
      notify.success({ message: 'Success', description: res.globalMessages[0].message }, '/logout');
    } else {
      handleResponse(res);
    }
  };

  return (
    <div>
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
            <div className={styles.profileHeader}>
              <Avatar
                size={120}
                icon={<UserOutlined />}
                className={`brand-avatar ${styles.profileAvatar}`}
              />
              <div>
                <Title level={2} className={styles.profileName}>
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
              labelStyle={{ fontWeight: 600, color: 'var(--text-secondary)' }}
              contentStyle={{ color: 'var(--priColor)' }}
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
              type="default"
              ghost
              icon={<LockOutlined />}
              onClick={() => setIsModalOpen(true)}
              className="outline-action-btn"
              style={{ marginTop: 16 }}
            >
              Change Password
            </Button>
          </Card>
        </Col>
      </Row>
      <ChangePasswordModal
        open={isModalOpen}
        onCancel={() => setIsModalOpen(false)}
        onSubmit={handleSubmit}
      />
    </div>
  );
}