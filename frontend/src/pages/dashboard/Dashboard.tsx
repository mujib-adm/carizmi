import { DashboardOutlined, DollarOutlined, TeamOutlined } from "@ant-design/icons";
import { Card, Col, Row, Table, Typography } from "antd";
import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { Cell, Pie, PieChart, ResponsiveContainer, Tooltip } from 'recharts';
import { getDashboardMetrics, getLatestPayments } from "../../apiclient/dashboardApi";
import { MessageBanner } from "../../component/MessageBanner";
import Sidebar from "../../component/Sidebar";
import { ReferenceCodeConstants } from "../../constants/ReferenceCodeConstants";
import { DashboardMetrics, RecentTransactions } from "../../constants/types";
import { useReference } from "../../context/ReferenceContext";
import { useApiMessages } from "../../hook/ApiResponseHandler";
import "./Dashboard.css";

const { Title } = Typography;

// Mock data for Quarterly Membership Fee Visualization
// Percentage of total membership fees collected relative to the total number of Active members
const COLORS = ['#2D6A4F', '#40916C', '#52B788', '#74C69D'];

const RADIAN = Math.PI / 180;
const renderCustomizedLabel = ({ cx, cy, midAngle, innerRadius, outerRadius, payload }: any) => {
  if (payload.isGap || payload.value === 0 || payload.name?.includes('Future') || payload.name?.includes('Unpaid')) return null;

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
        fill="#1E5631"
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
  const [metrics, setMetrics] = useState<DashboardMetrics>({
    totalMembers: 0,
    totalRevenue: 0,
    duesThisQuarter: 0,
    overdueTotal: 0,
    quarterlyFeeAmt: 0,
    quarterlyCollections: []
  });
  const [payments, setPayments] = useState<RecentTransactions[]>([]);
  const [loading, setLoading] = useState(true);
  const { toDisplay } = useReference();
  const { globalMessages, handleError, resetMessages } = useApiMessages<any>();

  useEffect(() => {
    const fetchDashboard = async () => {
      try {
        resetMessages();
        const metricsRes = await getDashboardMetrics();
        if (metricsRes.responseData) {
          setMetrics(metricsRes.responseData);
        }

        const paymentsRes = await getLatestPayments();
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
    { title: 'Date', dataIndex: 'paymentDate', key: 'paymentDate', render: (d: string) => d ? new Date(d).toLocaleDateString() : 'N/A' },
    { title: 'Member Name', dataIndex: 'memberName', key: 'memberName', render: (text: string, record: RecentTransactions) => <Link to={`/members/${record.memberID}`}>{text}</Link> },
    { title: 'Description', dataIndex: 'feeType', key: 'feeType', render: (code: string) => toDisplay(ReferenceCodeConstants.FEE_TYPE.NAME, code) },
    { title: 'Amount', dataIndex: 'amount', key: 'amount', render: (val: number) => `$${val?.toFixed(2) ?? '0.00'}` },
  ];

  // Prepare Pie Data for 4 Part Visualization
  // We want: [Gap, Q1 Paid, Q1 Unpaid, Gap, Q2 Paid, Q2 Unpaid...]
  // Total Potential per Quarter is constant: Active * quarterlyFeeAmt
  const activeMembers = metrics.totalMembers || 0;
  const potentialPerQ = activeMembers * metrics.quarterlyFeeAmt;
  const gapSize = potentialPerQ * 0.03; // Gap size (3%)

  const pieData: any[] = [];

  metrics.quarterlyCollections.forEach((q, idx) => {
    // 1. Spacer (Gap) - Transparent, to separate quarters
    pieData.push({
      name: '',
      value: gapSize,
      color: 'transparent',
      isGap: true
    });

    // 2. Paid Slice
    // If Future, Collected = 0.
    pieData.push({
      name: `${q.quarterLabel} Paid`,
      value: q.collectedAmount,
      rate: q.percentage,
      color: COLORS[idx % COLORS.length],
      isGap: false
    });

    // 3. Unpaid/Future Slice
    let unpaidVal = Math.max(0, potentialPerQ - q.collectedAmount);
    let unpaidColor = 'orange'; // Orange for Unpaid
    let unpaidName = `${q.quarterLabel} Unpaid`;

    if (q.status === 'FUTURE') {
      unpaidColor = '#E0E0E0'; // Gray for Future
      unpaidName = `${q.quarterLabel} Future`;
    }

    pieData.push({
      name: unpaidName,
      value: unpaidVal,
      rate: 0,
      color: unpaidColor,
      isGap: false
    });
  });

  // Center Label: Current Quarter Rate
  const currentQRate = metrics.quarterlyCollections.find(q => q.status === 'CURRENT')?.percentage ?? 0;

  return (
    <div className="dashboard-layout">
      <Sidebar />

      <main className="content fade-in">
        <div style={{ padding: 24 }}>
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
                <div className="metric-value">{metrics.totalMembers.toLocaleString()}</div>
                <div className="metric-subtext"> Total Active Members</div>
                <TeamOutlined style={{ position: 'absolute', bottom: 20, right: 20, fontSize: 48, color: '#1E5631', opacity: 0.15 }} />
              </Card>
            </Col>
            <Col xs={24} md={6}>
              <Card className="glass-card">
                <div className="metric-label">Total Revenue</div>
                <div className="metric-value">${metrics.totalRevenue?.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 }) ?? '0.00'}</div>
                <div className="metric-subtext"> Total Amount in account </div>
                <DollarOutlined style={{ position: 'absolute', bottom: 20, right: 20, fontSize: 48, color: '#40916C', opacity: 0.15 }} />
              </Card>
            </Col>
            <Col xs={24} md={6}>
              <Card className="glass-card">
                <div className="metric-label" style={{ color: 'orange' }}>Dues</div>
                <div className="metric-value" style={{ color: 'orange' }}>${metrics.duesThisQuarter?.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 }) ?? '0.00'}</div>
                <div className="metric-subtext"> Unpaid - Current Quarter </div>
                <DollarOutlined style={{ position: 'absolute', bottom: 20, right: 20, fontSize: 48, color: 'orange', opacity: 0.15 }} />
              </Card>
            </Col>
            <Col xs={24} md={6}>
              <Card className="glass-card">
                <div className="metric-label" style={{ color: 'red' }}>Overdues</div>
                <div className="metric-value" style={{ color: 'red' }}>${metrics.overdueTotal?.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 }) ?? '0.00'}</div>
                <div className="metric-subtext"> Unpaid - Overall </div>
                <DollarOutlined style={{ position: 'absolute', bottom: 20, right: 20, fontSize: 48, color: 'red', opacity: 0.15 }} />
              </Card>
            </Col>
          </Row>

          {/* Second Row — Quarterly Membership Fee Visualization & Recent Payments */}
          <Row gutter={[24, 24]} style={{ marginBottom: 32 }}>
            <Col xs={24} md={12}>
              <Card className="glass-card" style={{ height: '100%' }}>
                <div className="chart-container" style={{ position: 'relative' }}>
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
                          contentStyle={{ borderRadius: '12px', border: 'none', boxShadow: '0 4px 12px rgba(0,0,0,0.1)' }}
                          formatter={(value: any, name: any) => [`$${value?.toLocaleString() ?? '0'}`, name]}
                        />
                      </PieChart>
                    </ResponsiveContainer>
                    {/* Central Statistic */}
                    <div style={{
                      position: 'absolute',
                      top: '50%',
                      left: '50%',
                      transform: 'translate(-50%, -50%)',
                      textAlign: 'center',
                      pointerEvents: 'none'
                    }}>
                      <div style={{ fontSize: '24px', color: '#1E5631', fontWeight: 800, fontFamily: 'Outfit, sans-serif' }}>
                        {(currentQRate * 100).toFixed(1)}%
                      </div>
                    </div>
                  </div>

                  {/* Custom Legend */}
                  <div style={{ display: 'flex', justifyContent: 'center', gap: '24px', marginTop: '4px' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                      <div style={{ width: 12, height: 12, backgroundColor: '#40916C', borderRadius: 2 }}></div>
                      <span style={{ fontSize: '0.9rem', color: '#1E5631', fontWeight: 500 }}>Paid</span>
                    </div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                      <div style={{ width: 12, height: 12, backgroundColor: 'orange', borderRadius: 2 }}></div>
                      <span style={{ fontSize: '0.9rem', color: '#52665D', fontWeight: 500 }}>Unpaid</span>
                    </div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                      <div style={{ width: 12, height: 12, backgroundColor: '#E0E0E0', borderRadius: 2 }}></div>
                      <span style={{ fontSize: '0.9rem', color: '#999', fontWeight: 500 }}>Future</span>
                    </div>
                  </div>
                </div>
              </Card>
            </Col>
            <Col xs={24} md={12}>
              <Card className="glass-card" style={{ height: '100%' }}>
                <div className="chart-container" style={{ position: 'relative' }}>
                  <div className="chart-title" style={{ marginBottom: '28px' }}>Recent Payments</div>
                  <Table
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
      </main>
    </div>
  );
}