import { DashboardOutlined, DollarOutlined } from "@ant-design/icons";
import { Card, Col, Row, Table, Typography } from "antd";
import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { Cell, Legend, Pie, PieChart, ResponsiveContainer, Tooltip } from 'recharts';
import apiClient from "../../apiclient/ApiClient";
import Sidebar from "../../component/Sidebar";
import { FEE_TYPE } from "../../constants/referenceConstants";
import { DashboardMetrics, RecentTransactions } from "../../constants/types";
import { useReference } from "../../context/ReferenceContext";
import "./Dashboard.css";

const { Title } = Typography;

// Mock data for Quarterly Membership Fee Visualization
// Percentage of total membership fees collected relative to the total number of Active members
const COLORS = ['#2D6A4F', '#40916C', '#52B788', '#74C69D'];

const initialQuarterlyData = [
  { name: 'Q4 (Current)', value: 85, color: COLORS[0] },
  { name: 'Q3', value: 92, color: COLORS[1] },
  { name: 'Q2', value: 78, color: COLORS[2] },
  { name: 'Q1', value: 95, color: COLORS[3] },
];

const RADIAN = Math.PI / 180;
const renderCustomizedLabel = ({ cx, cy, midAngle, innerRadius, outerRadius, percent }: any) => {
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
        {`${(percent * 100).toFixed(0)}%`}
      </text>
    </g>
  );
};

export default function Dashboard() {
  const [metrics, setMetrics] = useState<DashboardMetrics>({ totalMembers: 0, totalRevenue: 0, duesThisQuarter: 0, overdueTotal: 0 });
  const [payments, setPayments] = useState<RecentTransactions[]>([]);
  const [loading, setLoading] = useState(true);
  const { toDisplay } = useReference();

  useEffect(() => {
    const fetchDashboard = async () => {
      try {
        const metricsRes = await apiClient.get("/dashboard/metrics");
        if (metricsRes.data.responseData) {
          setMetrics(metricsRes.data.responseData);
        }

        const paymentsRes = await apiClient.get("/payments/latest");
        if (paymentsRes.data.responseData) {
          setPayments(paymentsRes.data.responseData);
        }
      } catch (err) {
        console.error("Dashboard fetch failed", err);
      } finally {
        setLoading(false);
      }
    };
    fetchDashboard();
  }, []);

  const columns = [
    { title: 'Date', dataIndex: 'paymentDate', key: 'paymentDate', render: (d: string) => d ? new Date(d).toLocaleDateString() : 'N/A' },
    { title: 'Member Name', dataIndex: 'memberName', key: 'memberName', render: (text: string, record: RecentTransactions) => <Link to={`/members/${record.memberID}`}>{text}</Link> },
    { title: 'Description', dataIndex: 'feeType', key: 'feeType', render: (code: string) => toDisplay(FEE_TYPE, code) },
    { title: 'Amount', dataIndex: 'amount', key: 'amount', render: (val: number) => `$${val?.toFixed(2) ?? '0.00'}` },
  ];

  return (
    <div className="dashboard-layout">
      <Sidebar />

      <main className="content fade-in">
        <div style={{ padding: 24 }}>
          {/* <div style={{ marginBottom: 40 }}>
            <Title level={2} style={{ margin: 0, color: '#1E5631', fontFamily: 'Federo, sans-serif' }}>
              <DashboardOutlined /> Dashboard
            </Title>
            <Typography.Text style={{ color: '#52665D' }}>Membership portal overview</Typography.Text>
          </div> */}
          <div className="page-header">
            <Title level={2} className="page-title">
              <DashboardOutlined /> Dashboard
            </Title>
          </div>

          {/* First Row — Summary Metric Cards */}
          <Row gutter={[24, 24]} style={{ marginBottom: 32 }}>
            <Col xs={24} md={6}>
              <Card className="glass-card">
                <div className="metric-label">Total Members</div>
                <div className="metric-value">{metrics.totalMembers.toLocaleString()}</div>
                <div className="metric-subtext"> Total Active Members
                  {/* <span className="metric-trend-up">↑ 12%</span> vs last month */}
                </div>
                {/* Decorative wave placeholder */}
                <svg className="metric-wave" viewBox="0 0 1440 320" preserveAspectRatio="none">
                  <path fill="#40916C" fillOpacity="0.1" d="M0,192L48,197.3C96,203,192,213,288,192C384,171,480,117,576,112C672,107,768,149,864,165.3C960,181,1056,171,1152,149.3C1248,128,1344,96,1392,80L1440,64L1440,320L1392,320C1344,320,1248,320,1152,320C1056,320,960,320,864,320C768,320,672,320,576,320C480,320,384,320,288,320C192,320,96,320,48,320L0,320Z"></path>
                </svg>
              </Card>
            </Col>
            <Col xs={24} md={6}>
              <Card className="glass-card">
                <div className="metric-label">Total Revenue</div>
                <div className="metric-value">$0.00</div>
                <div className="metric-subtext"> Total Amount in account </div>
                <DollarOutlined style={{ position: 'absolute', bottom: 20, right: 20, fontSize: 48, color: '#40916C', opacity: 0.1 }} />
              </Card>
            </Col>
            <Col xs={24} md={6}>
              <Card className="glass-card">
                <div className="metric-label" style={{ color: 'orange' }}>Dues</div>
                <div className="metric-value" style={{ color: 'orange' }}>$0.00</div>
                <div className="metric-subtext"> Unpaid - Current Quarter </div>
                <DollarOutlined style={{ position: 'absolute', bottom: 20, right: 20, fontSize: 48, color: 'orange', opacity: 0.1 }} />
              </Card>
            </Col>
            <Col xs={24} md={6}>
              <Card className="glass-card">
                <div className="metric-label" style={{ color: 'red' }}>Overdues</div>
                <div className="metric-value" style={{ color: 'red' }}>$0.00</div>
                <div className="metric-subtext"> Unpaid - Overall </div>
                <DollarOutlined style={{ position: 'absolute', bottom: 20, right: 20, fontSize: 48, color: 'red', opacity: 0.1 }} />
              </Card>
            </Col>
          </Row>

          {/* Second Row — Quarterly Membership Fee Visualization */}
          <Row gutter={[24, 24]} style={{ marginBottom: 32 }}>
            <Col xs={24} md={12}>
              <Card className="glass-card">
                <div className="chart-container" style={{ position: 'relative' }}>
                  <div className="chart-title">Quarterly Fee Collection (%)</div>
                  <div style={{ width: '100%', height: 350, position: 'relative' }}>
                    <ResponsiveContainer>
                      <PieChart>
                        <Pie
                          data={initialQuarterlyData}
                          cx="50%"
                          cy="50%"
                          labelLine={false}
                          label={renderCustomizedLabel}
                          innerRadius="35%"
                          outerRadius="75%"
                          paddingAngle={6}
                          dataKey="value"
                          stroke="none"
                        >
                          {initialQuarterlyData.map((entry, index) => (
                            <Cell key={`cell-${index}`} fill={entry.color} />
                          ))}
                        </Pie>
                        <Tooltip
                          contentStyle={{ borderRadius: '12px', border: 'none', boxShadow: '0 4px 12px rgba(0,0,0,0.1)' }}
                          formatter={(value?: number) => [`${value ?? 0}% Collected`, 'Rate']}
                        />
                        <Legend verticalAlign="bottom" height={36} />
                      </PieChart>
                    </ResponsiveContainer>
                    {/* Central Statistic */}
                    <div style={{
                      position: 'absolute',
                      top: '50%',
                      left: '50%',
                      transform: 'translate(-50%, -90%)',
                      textAlign: 'center',
                      pointerEvents: 'none'
                    }}>
                      <div style={{ fontSize: '24px', color: '#1E5631', fontWeight: 800, fontFamily: 'Outfit, sans-serif' }}>87.5%</div>
                    </div>
                  </div>
                  <div style={{ marginTop: 16, textAlign: 'center', color: '#52665D', fontSize: '0.85rem' }}>
                    Visualizing fee collection relative to active members across 4 quarters
                  </div>
                </div>
              </Card>
            </Col>
            <Col xs={24} md={12}>
              <Card className="glass-card">
                <div className="chart-container" style={{ position: 'relative', height: 420 }}>
                  <div className="chart-title" style={{ marginBottom: '28px' }}>Recent Transactions</div>
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