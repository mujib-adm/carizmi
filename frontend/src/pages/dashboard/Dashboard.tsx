import { DashboardOutlined, DollarOutlined, TeamOutlined } from '@ant-design/icons';
import { Card, Col, Row, Table, Typography } from 'antd';
import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Cell, Pie, PieChart, ResponsiveContainer, Tooltip } from 'recharts';
import { dashboardApi } from '../../api/generated/dashboard/dashboard';
import { paymentsApi } from '../../api/generated/payments/payments';
import { MessageBanner } from '../../components/MessageBanner';
import { ReferenceConstants } from '../../constants/ReferenceConstants';
import { DashboardMetricsDto, LatestPaymentDto, QuarterStatus } from '../../api/generated/types';
import { useReference } from '../../hooks/useReference';
import { useApiMessages } from '../../hooks/useApiMessages';
import styles from '../../styles/pages/Dashboard.module.css';

const { Title } = Typography;

// Quarterly Membership Fee Visualization
// Percentage of total membership fees collected relative to the total number of Active members
const COLORS = ['#2D6A4F', '#40916C', '#52B788', '#74C69D'];

const RADIAN = Math.PI / 180;
const renderCustomizedLabel = ({ cx, cy, midAngle, outerRadius, payload }: any) => {
  if (
    payload.isGap ||
    payload.value === 0 ||
    payload.name?.includes('Future') ||
    payload.name?.includes('Dues') ||
    payload.name?.includes('Overdues')
  )
    return null;

  const radius = outerRadius + 25; // Move labels further out for the circled look
  const x = cx + radius * Math.cos(-midAngle * RADIAN);
  const y = cy + radius * Math.sin(-midAngle * RADIAN);

  return (
    <g>
      <circle
        cx={x}
        cy={y}
        r="20"
        fill="white"
        stroke="#40916C"
        strokeWidth="2"
        style={{ filter: 'drop-shadow(0px 4px 8px rgba(0,0,0,0.12))' }}
      />
      <text
        x={x}
        y={y}
        fill="#2f744e"
        textAnchor="middle"
        dominantBaseline="central"
        style={{ fontWeight: 800, fontSize: '13px', fontFamily: 'Outfit, sans-serif' }}
      >
        {`${(payload.rate * 100).toFixed(0)}%`}
      </text>
    </g>
  );
};

export default function Dashboard() {
  const [metrics, setMetrics] = useState<DashboardMetricsDto>({
    totalMembers: 0,
    totalRevenue: 0,
    duesThisQuarter: 0,
    overdueTotal: 0,
    quarterlyFeeAmt: 0,
    quarterlyCollections: [],
  });
  const [payments, setPayments] = useState<LatestPaymentDto[]>([]);
  const [loading, setLoading] = useState(true);
  const { toDisplay } = useReference();
  const { globalMessages, handleError, resetMessages } = useApiMessages<any>();

  useEffect(() => {
    const fetchDashboard = async () => {
      try {
        resetMessages();
        const [metricsRes, paymentsRes] = await Promise.all([
          dashboardApi.getMetrics(),
          paymentsApi.latestPayments(),
        ]);

        if (metricsRes.responseData) {
          setMetrics(metricsRes.responseData);
        }

        if (paymentsRes.responseData) {
          setPayments(paymentsRes.responseData);
        }
      } catch (err: any) {
        handleError(err);
      } finally {
        setLoading(false);
      }
    };
    fetchDashboard();
  }, []);

  const columns = [
    {
      title: 'Date',
      dataIndex: 'paymentDate',
      key: 'paymentDate',
      render: (d: string) => (d ? new Date(d).toLocaleDateString() : 'N/A'),
    },
    {
      title: 'Member Name',
      dataIndex: 'memberName',
      key: 'memberName',
      render: (text: string, record: LatestPaymentDto) => (
        <Link to={`/members/${record.memberID}`}>{text}</Link>
      ),
    },
    {
      title: 'Description',
      dataIndex: 'feeType',
      key: 'feeType',
      render: (code: string) => toDisplay(ReferenceConstants.FEE_TYPE.NAME, code),
    },
    {
      title: 'Amount',
      dataIndex: 'amount',
      key: 'amount',
      render: (val: number) => `$${val?.toFixed(2) ?? '0.00'}`,
    },
  ];

  // Prepare Pie Data for 4 Part Visualization
  const activeMembers = metrics.totalMembers || 0;
  const potentialPerQ = activeMembers * (metrics.quarterlyFeeAmt || 0);
  const gapSize = potentialPerQ * 0.03; // Gap size (3%)

  const pieData: any[] = [];

  (metrics.quarterlyCollections || []).forEach((q, idx) => {
    // 1. Spacer (Gap) - Transparent, to separate quarters
    pieData.push({
      name: '',
      value: gapSize,
      color: 'transparent',
      isGap: true,
    });

    // 2. Paid Slice
    // If Future, Collected = 0.
    pieData.push({
      name: `${q.quarterLabel} Paid`,
      value: q.collectedAmount || 0,
      rate: q.percentage,
      color: COLORS[idx % COLORS.length],
      isGap: false,
    });

    // 3. Dues/Overdues/Future Slice
    let unpaidVal = Math.max(0, potentialPerQ - (q.collectedAmount || 0));
    let unpaidColor = 'orange'; // Orange for current-quarter Dues
    let unpaidName = `${q.quarterLabel} Dues`;

    if (q.status === QuarterStatus.PAST) {
      unpaidColor = 'red'; // Red for past-quarter Overdues
      unpaidName = `${q.quarterLabel} Overdues`;
    } else if (q.status === QuarterStatus.FUTURE) {
      unpaidColor = '#E0E0E0'; // Gray for Future
      unpaidName = `${q.quarterLabel} Future`;
    }

    pieData.push({
      name: unpaidName,
      value: unpaidVal,
      rate: 0,
      color: unpaidColor,
      isGap: false,
    });
  });

  // Center Label: Current Quarter Rate
  const currentQRate = (metrics.quarterlyCollections || []).find((q) => q.status === QuarterStatus.CURRENT)?.percentage ?? 0;

  return (
    <div>
      <div className="page-header">
        <Title level={2} className="page-title">
          <DashboardOutlined /> Dashboard
        </Title>
      </div>

      {globalMessages && <MessageBanner messages={globalMessages} />}

      {/* First Row — Summary Metric Cards */}
      <Row gutter={[24, 24]} style={{ marginBottom: 32 }}>
        <Col xs={24} md={6}>
          <Card className="glass-card">
            <div className="metric-label">Total Members</div>
            <div className="metric-value">{(metrics.totalMembers || 0).toLocaleString()}</div>
            <div className="metric-subtext"> Total Active Members</div>
            <TeamOutlined
              style={{
                position: 'absolute',
                bottom: 20,
                right: 20,
                fontSize: 48,
                color: '#2f744e',
                opacity: 0.15,
              }}
            />
          </Card>
        </Col>
        <Col xs={24} md={6}>
          <Card className="glass-card">
            <div className="metric-label">Total Revenue</div>
            <div className="metric-value">
              $
              {metrics.totalRevenue?.toLocaleString(undefined, {
                minimumFractionDigits: 2,
                maximumFractionDigits: 2,
              }) ?? '0.00'}
            </div>
            <div className="metric-subtext"> Total Amount in Account </div>
            <DollarOutlined
              style={{
                position: 'absolute',
                bottom: 20,
                right: 20,
                fontSize: 48,
                color: '#40916C',
                opacity: 0.15,
              }}
            />
          </Card>
        </Col>
        <Col xs={24} md={6}>
          <Card className="glass-card">
            <div className="metric-label" style={{ color: 'orange' }}>
              Dues
            </div>
            <div className="metric-value" style={{ color: 'orange' }}>
              $
              {metrics.duesThisQuarter?.toLocaleString(undefined, {
                minimumFractionDigits: 2,
                maximumFractionDigits: 2,
              }) ?? '0.00'}
            </div>
            <div className="metric-subtext"> Unpaid - Current Quarter </div>
            <DollarOutlined
              style={{
                position: 'absolute',
                bottom: 20,
                right: 20,
                fontSize: 48,
                color: 'orange',
                opacity: 0.15,
              }}
            />
          </Card>
        </Col>
        <Col xs={24} md={6}>
          <Card className="glass-card">
            <div className="metric-label" style={{ color: 'red' }}>
              Overdues
            </div>
            <div className="metric-value" style={{ color: 'red' }}>
              $
              {metrics.overdueTotal?.toLocaleString(undefined, {
                minimumFractionDigits: 2,
                maximumFractionDigits: 2,
              }) ?? '0.00'}
            </div>
            <div className="metric-subtext"> Unpaid - Past Quarter(s) </div>
            <DollarOutlined
              style={{
                position: 'absolute',
                bottom: 20,
                right: 20,
                fontSize: 48,
                color: 'red',
                opacity: 0.15,
              }}
            />
          </Card>
        </Col>
      </Row>

      {/* Second Row — Quarterly Membership Fee Visualization & Recent Payments */}
      <Row gutter={[24, 24]} style={{ marginBottom: 32 }}>
        <Col xs={24} md={12}>
          <Card className="glass-card" style={{ height: '100%' }}>
            <div className={styles.chartContainer} style={{ position: 'relative' }}>
              <div className="chart-title">Quarterly Fee Collection (%)</div>
              <div style={{ width: '100%', height: 350, position: 'relative' }}>
                <ResponsiveContainer>
                  <PieChart>
                    <Pie
                      data={pieData}
                      cx="50%"
                      cy="50%"
                      labelLine={false}
                      label={renderCustomizedLabel}
                      innerRadius="35%"
                      outerRadius="75%"
                      startAngle={90}
                      endAngle={-270}
                      dataKey="value"
                      stroke="none"
                    >
                      {pieData.map((entry, index) => (
                        <Cell key={`cell-${index}`} fill={entry.color} />
                      ))}
                    </Pie>
                    <Tooltip
                      content={({ active, payload }) => {
                        if (!active || !payload?.length || payload[0]?.payload?.isGap) return null;
                        const entry = payload[0];
                        const sliceColor = entry.payload?.color || '#40916C';
                        const formatted = Number(entry.value).toLocaleString(undefined, {
                          minimumFractionDigits: 2,
                          maximumFractionDigits: 2,
                        });
                        return (
                          <div
                            style={{
                              background: '#1a3a2a',
                              borderRadius: '10px',
                              padding: '10px 16px',
                              borderLeft: `4px solid ${sliceColor}`,
                              boxShadow: '0 8px 24px rgba(0,0,0,0.25)',
                              fontSize: '0.85rem',
                              lineHeight: 1.7,
                              minWidth: '120px',
                            }}
                          >
                            <div style={{
                              display: 'flex',
                              alignItems: 'center',
                              gap: '8px',
                              fontWeight: 600,
                              color: '#fff',
                            }}>
                              <span style={{
                                width: 8,
                                height: 8,
                                borderRadius: '50%',
                                backgroundColor: sliceColor,
                                display: 'inline-block',
                                flexShrink: 0,
                              }} />
                              {entry.name}
                            </div>
                            <div style={{
                              color: 'rgba(255,255,255,0.75)',
                              fontFamily: 'Outfit, sans-serif',
                              fontWeight: 700,
                              fontSize: '1rem',
                              paddingLeft: '16px',
                            }}>
                              ${formatted}
                            </div>
                          </div>
                        );
                      }}
                      wrapperStyle={{ zIndex: 10 }}
                    />
                  </PieChart>
                </ResponsiveContainer>
                {/* Central Statistic */}
                <div
                  style={{
                    position: 'absolute',
                    top: '50%',
                    left: '50%',
                    transform: 'translate(-50%, -50%)',
                    textAlign: 'center',
                    pointerEvents: 'none',
                  }}
                >
                  <div
                    style={{
                      fontSize: '24px',
                      color: '#2f744e',
                      fontWeight: 800,
                      fontFamily: 'Outfit, sans-serif',
                      lineHeight: 1.2,
                    }}
                  >
                    {(currentQRate * 100).toFixed(1)}%
                  </div>
                  <div
                    style={{
                      fontSize: '11px',
                      color: '#52665D',
                      fontWeight: 600,
                      fontFamily: 'Poppins, sans-serif',
                      letterSpacing: '0.5px',
                      textTransform: 'uppercase',
                      marginTop: '2px',
                    }}
                  >
                    Current Qtr
                  </div>
                </div>
              </div>

              {/* Custom Legend */}
              <div
                style={{ display: 'flex', justifyContent: 'center', gap: '20px', marginTop: '4px', flexWrap: 'wrap' }}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <div
                    style={{ width: 12, height: 12, backgroundColor: '#2f744e', borderRadius: 2 }}
                  ></div>
                  <span style={{ fontSize: '0.9rem', color: '#2f744e', fontWeight: 500 }}>
                    Paid
                  </span>
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <div
                    style={{ width: 12, height: 12, backgroundColor: 'orange', borderRadius: 2 }}
                  ></div>
                  <span style={{ fontSize: '0.9rem', color: 'orange', fontWeight: 500 }}>
                    Dues
                  </span>
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <div
                    style={{ width: 12, height: 12, backgroundColor: 'red', borderRadius: 2 }}
                  ></div>
                  <span style={{ fontSize: '0.9rem', color: 'red', fontWeight: 500 }}>
                    Overdues
                  </span>
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <div
                    style={{ width: 12, height: 12, backgroundColor: '#E0E0E0', borderRadius: 2 }}
                  ></div>
                  <span style={{ fontSize: '0.9rem', color: '#999', fontWeight: 500 }}>Future</span>
                </div>
              </div>
            </div>
          </Card>
        </Col>
        <Col xs={24} md={12}>
          <Card className="glass-card" style={{ height: '100%' }}>
            <div className={styles.chartContainer} style={{ position: 'relative' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '28px' }}>
                <div className="chart-title">Recent Payments</div>
                <Link to="/payments" state={{ fromDashboard: true }} style={{ fontSize: '0.85rem', fontWeight: 600, color: '#40916C' }}>
                  View All →
                </Link>
              </div>
              <Table
                scroll={{ x: 'max-content' }}
                columns={columns}
                dataSource={payments}
                rowKey="paymentID"
                loading={loading}
                pagination={false}
                size="small"
              />
            </div>
          </Card>
        </Col>
      </Row>
    </div>
  );
}