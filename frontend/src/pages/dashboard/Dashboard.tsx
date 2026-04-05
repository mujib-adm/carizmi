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
import { useTheme } from '../../hooks/useTheme';
import { getPaidColor, getUnpaidColor } from '../../constants/ChartThemeConstants';
import { ChartGradientDefs, createLabelRenderer } from '../../components/chart/ChartDefs';
import styles from '../../styles/pages/Dashboard.module.css';

const { Title } = Typography;


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
  const { theme } = useTheme();
  const isDark = theme === 'dark';

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
      color: getPaidColor(isDark, idx),
      isGap: false,
    });

    // 3. Dues/Overdues/Future Slice
    let unpaidVal = Math.max(0, potentialPerQ - (q.collectedAmount || 0));
    let unpaidColor = getUnpaidColor(isDark, 'dues');
    let unpaidName = `${q.quarterLabel} Dues`;

    if (q.status === QuarterStatus.PAST) {
      unpaidColor = getUnpaidColor(isDark, 'overdue');
      unpaidName = `${q.quarterLabel} Overdues`;
    } else if (q.status === QuarterStatus.FUTURE) {
      unpaidColor = getUnpaidColor(isDark, 'future');
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
          <Card className="glass-card" data-metric="members">
            <div className="metric-label">Total Members</div>
            <div className="metric-value">{(metrics.totalMembers || 0).toLocaleString()}</div>
            <div className="metric-subtext"> Total Active Members</div>
            <TeamOutlined className={styles.metricIconDefault} />
          </Card>
        </Col>
        <Col xs={24} md={6}>
          <Card className="glass-card" data-metric="revenue">
            <div className="metric-label">Total Revenue</div>
            <div className="metric-value">
              $
              {metrics.totalRevenue?.toLocaleString(undefined, {
                minimumFractionDigits: 2,
                maximumFractionDigits: 2,
              }) ?? '0.00'}
            </div>
            <div className="metric-subtext"> Total Amount in Account </div>
            <DollarOutlined className={styles.metricIconRevenue} />
          </Card>
        </Col>
        <Col xs={24} md={6}>
          <Card className="glass-card" data-metric="dues">
            <div className={`metric-label ${styles.metricDues}`}>
              Dues
            </div>
            <div className={`metric-value ${styles.metricDues}`}>
              $
              {metrics.duesThisQuarter?.toLocaleString(undefined, {
                minimumFractionDigits: 2,
                maximumFractionDigits: 2,
              }) ?? '0.00'}
            </div>
            <div className="metric-subtext"> Unpaid - Current Quarter </div>
            <DollarOutlined className={styles.metricIconDues} />
          </Card>
        </Col>
        <Col xs={24} md={6}>
          <Card className="glass-card" data-metric="overdue">
            <div className={`metric-label ${styles.metricOverdue}`}>
              Overdues
            </div>
            <div className={`metric-value ${styles.metricOverdue}`}>
              $
              {metrics.overdueTotal?.toLocaleString(undefined, {
                minimumFractionDigits: 2,
                maximumFractionDigits: 2,
              }) ?? '0.00'}
            </div>
            <div className="metric-subtext"> Unpaid - Past Quarter(s) </div>
            <DollarOutlined className={styles.metricIconOverdue} />
          </Card>
        </Col>
      </Row>

      {/* Second Row — Quarterly Membership Fee Visualization & Recent Payments */}
      <Row gutter={[24, 24]} style={{ marginBottom: 32 }}>
        <Col xs={24} md={12}>
          <Card className="glass-card" data-panel="chart" style={{ height: '100%' }}>
            <div className={styles.chartContainer} style={{ position: 'relative' }}>
              <div className="chart-title">Quarterly Fee Collection (%)</div>
              <div style={{ width: '100%', height: 350, position: 'relative' }}>
                <ResponsiveContainer>
                  <PieChart>
                    {isDark && <ChartGradientDefs />}
                    <Pie
                      data={pieData}
                      cx="50%"
                      cy="50%"
                      labelLine={false}
                      label={createLabelRenderer(isDark)}
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
                          <div className={styles.tooltipBox} style={{ borderLeft: `4px solid ${sliceColor}` }}>
                            <div className={styles.tooltipLabel}>
                              <span className={styles.tooltipDot} style={{ backgroundColor: sliceColor }} />
                              {entry.name}
                            </div>
                            <div className={styles.tooltipValue}>
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
                <div className={styles.centerLabel}>
                  <div className={styles.centerRate}>
                    {(currentQRate * 100).toFixed(1)}%
                  </div>
                  <div className={styles.centerSubtext}>
                    Current Qtr
                  </div>
                </div>
              </div>

              {/* Custom Legend */}
              <div className={styles.legendContainer}>
                <div className={styles.legendItem}>
                  <div className={styles.legendSwatchPaid}></div>
                  <span className={styles.legendTextPaid}>Paid</span>
                </div>
                <div className={styles.legendItem}>
                  <div className={styles.legendSwatchDues}></div>
                  <span className={styles.legendTextDues}>Dues</span>
                </div>
                <div className={styles.legendItem}>
                  <div className={styles.legendSwatchOverdue}></div>
                  <span className={styles.legendTextOverdue}>Overdues</span>
                </div>
                <div className={styles.legendItem}>
                  <div className={styles.legendSwatchFuture}></div>
                  <span className={styles.legendTextFuture}>Future</span>
                </div>
              </div>
            </div>
          </Card>
        </Col>
        <Col xs={24} md={12}>
          <Card className="glass-card" data-panel="recent-payments" style={{ height: '100%' }}>
            <div className={styles.chartContainer} style={{ position: 'relative' }}>
              <div className={styles.recentHeader}>
                <div className="chart-title">Recent Payments</div>
                <Link to="/payments" state={{ fromDashboard: true }} className={styles.viewAllLink}>
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