import {
  ArrowLeftOutlined,
  DollarOutlined,
  ExclamationCircleOutlined,
  UserOutlined,
} from '@ant-design/icons';
import { Avatar, Badge, Button, Card, Col, Row, Skeleton, Space, Table, Typography } from 'antd';
import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { membersApi } from '../../../api/generated/members/members';
import { paymentsApi } from '../../../api/generated/payments/payments';
import { ReferenceConstants } from '../../../constants/ReferenceConstants';
import { MemberDto, MemberSummaryDto, PaymentDto } from '../../../api/generated/types';
import { useReference } from '../../../hooks/useReference';
import { useApiMessages } from '../../../hooks/useApiMessages';

const { Title, Text } = Typography;

export default function MemberDetailsPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { toDisplay } = useReference();
  const { handleError } = useApiMessages();

  const [member, setMember] = useState<MemberDto | null>(null);
  const [payments, setPayments] = useState<PaymentDto[]>([]);
  const [summary, setSummary] = useState<MemberSummaryDto | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchData = async () => {
      if (!id) return;
      try {
        const memberID = Number(id);
        const [memberData, paymentData, summaryData] = await Promise.all([
          membersApi.getMember(memberID),
          paymentsApi.searchPayments({ memberID, size: 10 }),
          membersApi.getMemberSummary(memberID),
        ]);
        setMember(memberData.responseData || null);
        setPayments(paymentData.responseData || []);
        setSummary(summaryData.responseData || null);
      } catch (e: any) {
        handleError(e);
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, [id]);

  const columns = [
    {
      title: 'Date',
      dataIndex: 'dateReceived',
      key: 'dateReceived',
      render: (d: string) => (d ? new Date(d).toLocaleDateString() : 'N/A'),
    },
    {
      title: 'Description',
      dataIndex: 'feeType',
      key: 'feeType',
      render: (code: string) => toDisplay(ReferenceConstants.FEE_TYPE.NAME, code),
    },
    {
      title: 'Method',
      dataIndex: 'methodOfPayment',
      key: 'methodOfPayment',
      render: (code: string) => toDisplay(ReferenceConstants.PAYMENT_METHOD.NAME, code),
    },
    {
      title: 'Amount',
      dataIndex: 'amount',
      key: 'amount',
      render: (val: number) => `$${val.toFixed(2)}`,
    },
  ];

  return (
    <div>
      <Space style={{ marginBottom: 24 }}>
        <Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/members')}>
          Back to Members
        </Button>
      </Space>

      {loading ? (
        <Skeleton active avatar paragraph={{ rows: 4 }} />
      ) : member ? (
        <div className="member-profile">
          <Card className="glass-card" data-panel="member-info" style={{ marginBottom: 12 }}>
            <Row align="middle" gutter={24}>
              <Col>
                <Avatar
                  size={100}
                  icon={<UserOutlined />}
                  className="brand-avatar"
                />
              </Col>
              <Col flex="auto">
                <Title level={2} className="title-wrap" style={{ margin: 0 }}>
                  {member.firstName} {member.lastName}
                </Title>
                <Text type="secondary">
                  {member.phone} {member.email ? `| ${member.email}` : ''}
                </Text>
                <div style={{ marginTop: 8 }}>
                  <Badge
                    status={member.status === 'ACTIVE' ? 'success' : 'default'}
                    text={toDisplay(ReferenceConstants.MEMBER_STATUS.NAME, member.status || '')}
                  />
                </div>
              </Col>
            </Row>
          </Card>

          <Row gutter={[24, 24]} style={{ marginBottom: 12 }}>
            <Col xs={24} sm={8}>
              <Card className="glass-card" data-metric="revenue">
                <Space align="center">
                  <Avatar
                    icon={<DollarOutlined />}
                    style={{ background: 'var(--vibrant-gradient)' }}
                  />
                  <div>
                    <div className="metric-label">Total Paid</div>
                    <div className="metric-value">${(summary?.totalPaid ?? 0).toFixed(2)}</div>
                    <div className="metric-subtext">Total contributions up to date</div>
                  </div>
                </Space>
              </Card>
            </Col>
            <Col xs={24} sm={8}>
              <Card className="glass-card" data-metric="dues">
                <Space align="center">
                  <Avatar icon={<ExclamationCircleOutlined />} className="status-avatar-dues" />
                  <div>
                    <div className="metric-label">Outstanding</div>
                    <div className="metric-value status-text-dues">
                      ${(summary?.outstanding ?? 0).toFixed(2)}
                    </div>
                    <div className="metric-subtext"> Unpaid - Current Quarter </div>
                  </div>
                </Space>
              </Card>
            </Col>
            <Col xs={24} sm={8}>
              <Card className="glass-card" data-metric="overdue">
                <Space align="center">
                  <Avatar icon={<ExclamationCircleOutlined />} className="status-avatar-overdue" />
                  <div>
                    <div className="metric-label">Overdues</div>
                    <div className="metric-value status-text-overdue">
                      ${(summary?.overdue ?? 0).toFixed(2)}
                    </div>
                    <div className="metric-subtext"> Unpaid - Past Quarter(s) </div>
                  </div>
                </Space>
              </Card>
            </Col>
          </Row>

          <Card className="glass-card" data-panel="recent-payments">
            <div className="chart-title" style={{ marginBottom: '28px' }}>
              Recent Payment History
            </div>
            <Table
              scroll={{ x: true }}
              dataSource={payments}
              columns={columns}
              rowKey="paymentID"
              pagination={{ pageSize: 5 }}
              size="small"
            />
          </Card>
        </div>
      ) : (
        <Text type="danger">Member not found.</Text>
      )}
    </div>
  );
}