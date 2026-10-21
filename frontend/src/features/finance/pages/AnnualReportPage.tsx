import React, { useMemo, useState } from 'react';
import {
  FileTextOutlined,
  PrinterOutlined,
  DollarCircleOutlined,
  DollarOutlined,
  TeamOutlined,
  ShoppingCartOutlined,
  BarChartOutlined,
  FileSearchOutlined,
} from '@ant-design/icons';
import { Button, Progress, Select, Space, Spin, Table, Tag, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { reportsApi } from '../../../api/generated/reports/reports';
import type {
  AnnualExpenseCategoryDto,
  AnnualFeeTypeReceiptDto,
  AnnualFinancialReportDto,
  AnnualMethodReceiptDto,
} from '../../../api/generated/types';
import { useReference } from '../../../hooks/useReference';
import { useFiscalYears } from '../../../hooks/useFiscalYears';
import { ReferenceConstants } from '../../../constants/ReferenceConstants';
import { useApiMessages } from '../../../hooks/useApiMessages';
import { MessageBanner } from '../../../components/MessageBanner';
import officialLogo from '../../../assets/images/official_logo.jpeg';
import styles from '../../../styles/pages/AnnualReportPage.module.css';

const { Title, Text } = Typography;

const formatCurrency = (val?: number | null): string => {
  return `$${(val ?? 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
};

const renderEfficiencyBadge = (rate?: number | null) => {
  const value = rate ?? 0;
  const isHealthy = value >= 80;
  const formattedPct = value.toFixed(1) === '100.0' ? '100%' : `${value.toFixed(1)}%`;
  return (
    <Tag
      color={isHealthy ? 'success' : 'warning'}
      className={isHealthy ? styles.efficiencyBadgeSuccess : styles.efficiencyBadgeWarning}
    >
      {formattedPct}
    </Tag>
  );
};

interface ComplianceRowItem {
  key: string;
  streamName: string;
  badgeLabel?: string;
  badgeColor?: string;
  memberBase: string;
  memberBaseSubnote?: string;
  rate: string;
  expectedAmount: number;
  actualCollected: number;
  efficiencyRate: number;
  overdueAmount: number;
}

export default function AnnualReportPage() {
  const { availableYears, yearOptions, currentYear } = useFiscalYears();
  const [selectedYear, setSelectedYear] = useState<number>(currentYear);
  const [report, setReport] = useState<AnnualFinancialReportDto | null>(null);
  const [loading, setLoading] = useState<boolean>(false);
  const { toDisplay } = useReference();
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const { globalMessages, handleError, resetMessages } = useApiMessages<any>();

  const fetchReport = (year: number) => {
    setLoading(true);
    resetMessages();
    reportsApi
      .getAnnualReport({ year })
      .then((resp) => {
        if (resp.responseData) {
          setReport(resp.responseData);
        }
      })
      .catch((err) => {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        handleError(err as any);
      })
      .finally(() => {
        setLoading(false);
      });
  };

  const handleYearChange = (year: number) => {
    setSelectedYear(year);
    if (report && report.year !== year) {
      setReport(null);
    }
  };

  const handleGenerateReport = () => {
    fetchReport(selectedYear);
  };

  const handlePrint = () => {
    window.print();
  };

  // 1. Fee Types Table Columns
  const feeTypeColumns: ColumnsType<AnnualFeeTypeReceiptDto> = [
    {
      title: 'Fee Type',
      dataIndex: 'feeType',
      key: 'feeType',
      render: (code: string) => toDisplay(ReferenceConstants.FEE_TYPE.NAME, code) || code,
    },
    {
      title: 'Q1',
      dataIndex: 'q1',
      key: 'q1',
      align: 'right',
      render: (val: number) => formatCurrency(val),
    },
    {
      title: 'Q2',
      dataIndex: 'q2',
      key: 'q2',
      align: 'right',
      render: (val: number) => formatCurrency(val),
    },
    {
      title: 'Q3',
      dataIndex: 'q3',
      key: 'q3',
      align: 'right',
      render: (val: number) => formatCurrency(val),
    },
    {
      title: 'Q4',
      dataIndex: 'q4',
      key: 'q4',
      align: 'right',
      render: (val: number) => formatCurrency(val),
    },
    {
      title: 'Total Collections',
      dataIndex: 'total',
      key: 'total',
      align: 'right',
      render: (val: number) => (
        <span className={styles.positiveAmount}>
          {formatCurrency(val)}
        </span>
      ),
    },
  ];

  // 2. Payment Method Columns
  const methodColumns: ColumnsType<AnnualMethodReceiptDto> = [
    {
      title: 'Payment Channel',
      dataIndex: 'methodOfPayment',
      key: 'methodOfPayment',
      render: (code: string) => toDisplay(ReferenceConstants.PAYMENT_METHOD.NAME, code) || code,
    },
    {
      title: 'Total Amount',
      dataIndex: 'totalAmount',
      key: 'totalAmount',
      align: 'right',
      render: (val: number) => formatCurrency(val),
    },
    {
      title: 'Share (%)',
      dataIndex: 'percentage',
      key: 'percentage',
      align: 'right',
      width: 140,
      render: (pct: number) => `${pct?.toFixed(1) ?? '0.0'}%`,
    },
    {
      title: 'Distribution',
      key: 'distribution',
      render: (_, record) => (
        <Progress
          percent={record.percentage ?? 0}
          size="small"
          strokeColor="var(--primary-color, #1e5631)"
          showInfo={false}
        />
      ),
    },
  ];

  // 3. Expense Category Columns
  const expenseColumns: ColumnsType<AnnualExpenseCategoryDto> = [
    {
      title: 'Expenditure Category',
      dataIndex: 'category',
      key: 'category',
      render: (code: string) => toDisplay(ReferenceConstants.EXPENSE_CATEGORY.NAME, code) || code,
    },
    {
      title: 'Disbursed Amount',
      dataIndex: 'totalAmount',
      key: 'totalAmount',
      align: 'right',
      render: (val: number) => (
        <span className={styles.negativeAmount}>{formatCurrency(val)}</span>
      ),
    },
    {
      title: 'Share (%)',
      dataIndex: 'percentage',
      key: 'percentage',
      align: 'right',
      width: 140,
      render: (pct: number) => `${pct?.toFixed(1) ?? '0.0'}%`,
    },
    {
      title: 'Distribution',
      key: 'distribution',
      render: (_, record) => (
        <Progress
          percent={record.percentage ?? 0}
          size="small"
          strokeColor="#b91c1c"
          showInfo={false}
        />
      ),
    },
  ];

  // 4. Dues Compliance & Inflow Health Columns
  const complianceColumns: ColumnsType<ComplianceRowItem> = [
    {
      title: 'Fee Type',
      dataIndex: 'streamName',
      key: 'streamName',
      width: '28%',
      render: (val: string, record) => (
        <div>
          <div>{val}</div>
          {record.badgeLabel && (
            <Tag color={record.badgeColor} className={styles.complianceTag}>
              {record.badgeLabel}
            </Tag>
          )}
        </div>
      ),
    },
    {
      title: 'Member Count',
      dataIndex: 'memberBase',
      key: 'memberBase',
      width: '20%',
      render: (val: string, record) => (
        <div>
          <div>{val}</div>
          {record.memberBaseSubnote && (
            <div className={styles.complianceSubnote}>
              {record.memberBaseSubnote}
            </div>
          )}
        </div>
      ),
    },
    {
      title: 'Rate',
      dataIndex: 'rate',
      key: 'rate',
      align: 'right',
      width: '12%',
      render: (val: string) => <span>{val}</span>,
    },
    {
      title: 'Expected',
      dataIndex: 'expectedAmount',
      key: 'expectedAmount',
      align: 'right',
      width: '13%',
      render: (val: number) => (
        <span className={styles.expectedAmount}>{formatCurrency(val)}</span>
      ),
    },
    {
      title: 'Collected',
      dataIndex: 'actualCollected',
      key: 'actualCollected',
      align: 'right',
      width: '13%',
      render: (val: number) => (
        <span className={styles.collectedAmount}>
          {formatCurrency(val)}
        </span>
      ),
    },
    {
      title: 'Collection %',
      dataIndex: 'efficiencyRate',
      key: 'efficiencyRate',
      align: 'center',
      width: '14%',
      render: (val: number) => (
        <div className={styles.progressStack}>
          {renderEfficiencyBadge(val)}
          <Progress
            percent={Math.min(val, 100)}
            size="small"
            strokeColor={val >= 80 ? 'var(--primary-color, #1e5631)' : '#f59e0b'}
            showInfo={false}
            className={styles.efficiencyProgressBar}
          />
        </div>
      ),
    },
    {
      title: 'Overdue',
      dataIndex: 'overdueAmount',
      key: 'overdueAmount',
      align: 'right',
      width: '14%',
      render: (val: number) => (
        <span className={val > 0 ? styles.overdueActive : styles.overdueZero}>
          {formatCurrency(val)}
        </span>
      ),
    },
  ];

  const complianceData: ComplianceRowItem[] = useMemo(() => {
    if (!report?.complianceSummary) return [];
    return [
      {
        key: 'membership',
        streamName: 'Membership Fee',
        badgeLabel: 'Quarterly Dues',
        badgeColor: 'blue',
        memberBase: `${
          report.complianceSummary?.membershipFeeSummary?.activeMembersCount ??
          report.complianceSummary?.activeMembersCount ??
          0
        } members`,
        memberBaseSubnote: `Prorated: Q1(${
          report.complianceSummary?.membershipFeeSummary?.q1EligibleMembers ?? 0
        }), Q2(${
          report.complianceSummary?.membershipFeeSummary?.q2EligibleMembers ?? 0
        }), Q3(${
          report.complianceSummary?.membershipFeeSummary?.q3EligibleMembers ?? 0
        }), Q4(${
          report.complianceSummary?.membershipFeeSummary?.q4EligibleMembers ?? 0
        })`,
        rate: `${formatCurrency(
          report.complianceSummary?.membershipFeeSummary?.quarterlyFeeRate ??
            report.complianceSummary?.quarterlyFeeRate
        )}`,
        expectedAmount:
          report.complianceSummary?.membershipFeeSummary?.annualExpectedDues ??
          report.complianceSummary?.annualExpectedDues ??
          0,
        actualCollected:
          report.complianceSummary?.membershipFeeSummary?.actualDuesCollected ??
          report.complianceSummary?.actualDuesCollected ??
          0,
        efficiencyRate:
          report.complianceSummary?.membershipFeeSummary?.collectionEfficiencyRate ??
          report.complianceSummary?.collectionEfficiencyRate ??
          0,
        overdueAmount:
          report.complianceSummary?.membershipFeeSummary?.totalOverdueDues ??
          report.complianceSummary?.totalOverdueDues ??
          0,
      },
      {
        key: 'registration',
        streamName: 'Registration Fee',
        badgeLabel: 'One-Time Fee',
        badgeColor: 'cyan',
        memberBase: `${
          report.complianceSummary?.registrationFeeSummary?.newMembersCount ?? 0
        } members`,
        memberBaseSubnote: `Joined: Q1(${
          report.complianceSummary?.registrationFeeSummary?.q1NewMembers ?? 0
        }), Q2(${
          report.complianceSummary?.registrationFeeSummary?.q2NewMembers ?? 0
        }), Q3(${
          report.complianceSummary?.registrationFeeSummary?.q3NewMembers ?? 0
        }), Q4(${
          report.complianceSummary?.registrationFeeSummary?.q4NewMembers ?? 0
        })`,
        rate: `${formatCurrency(
          report.complianceSummary?.registrationFeeSummary?.registrationFeeRate
        )}`,
        expectedAmount:
          report.complianceSummary?.registrationFeeSummary?.expectedRegistrationFees ?? 0,
        actualCollected:
          report.complianceSummary?.registrationFeeSummary?.actualRegistrationFeesCollected ?? 0,
        efficiencyRate:
          report.complianceSummary?.registrationFeeSummary?.collectionEfficiencyRate ?? 0,
        overdueAmount:
          report.complianceSummary?.registrationFeeSummary?.totalOverdueRegistrationFees ?? 0,
      },
    ];
  }, [report]);

  return (
    <div>
      {/* Screen Header Controls (Hidden during Print) */}
      <div className={`${styles.pageHeader} ${styles.noPrint}`}>
        <div className={styles.titleArea}>
          <Title level={2} className={`page-title ${styles.pageTitleHeading}`}>
            <FileTextOutlined /> Reports
          </Title>
        </div>

        <div className={styles.controlsArea}>
          <Space>
            <span className={styles.filterLabel}>Fiscal Year:</span>
            <Select
              value={selectedYear}
              onChange={handleYearChange}
              className={styles.yearSelect}
              options={yearOptions}
            />
            <Button
              type={report ? 'default' : 'primary'}
              icon={<BarChartOutlined />}
              onClick={handleGenerateReport}
              loading={loading}
              className={report ? undefined : styles.generateButton}
            >
              Generate Report
            </Button>
            <Button
              type="primary"
              icon={<PrinterOutlined />}
              onClick={handlePrint}
              disabled={!report || loading}
              className={styles.printButton}
            >
              Print / Save as PDF
            </Button>
          </Space>
        </div>
      </div>

      {globalMessages && <MessageBanner messages={globalMessages} />}

      {/* Main Report Document Sheet */}
      {loading ? (
        <div className={styles.loadingContainer}>
          <Spin size="large" />
          <div className={styles.loadingText}>
            Compiling financial report for {selectedYear}...
          </div>
        </div>
      ) : report ? (
        <div className={styles.reportSheet}>
          {/* Letterhead */}
          <div className={styles.letterhead}>
            <img
              src={officialLogo}
              alt="Sof'umar Community of Minnesota"
              className={styles.headerLogo}
            />
            <div className={styles.headerCenter}>
              <div className={styles.reportTitle}>Annual Financial Statement</div>
              <div className={styles.reportSubtitle}>
                Cash Receipts, Disbursements, and Fund Balance
              </div>
              <div className={styles.metaRow}>
                <span className={styles.metaItemCenter}>
                  <strong>For the Year Ended:</strong> December 31, {report.year}
                </span>
              </div>
            </div>
          </div>

          {/* Section 1: Cash Balance Reconciliation */}
          <div className={styles.section}>
            <div className={styles.sectionHeader}>
              <DollarCircleOutlined /> 1. Cash Balance Summary & Fund Reconciliation
            </div>

            <div className={styles.reconciliationGrid}>
              <div className={styles.reconCard}>
                <div className={styles.reconLabel}>
                  <span>Beginning Cash</span>
                  <span>(Jan 1)</span>
                </div>
                <div className={styles.reconValue}>
                  {formatCurrency(report.beginningCashBalance)}
                </div>
                <div className={styles.reconNote}>From prior year closing baseline</div>
              </div>

              <div className={styles.reconCard}>
                <div className={styles.reconLabel}>
                  <span>(+) Total</span>
                  <span>Receipts</span>
                </div>
                <div className={styles.reconValuePositive}>
                  {formatCurrency(report.totalReceipts)}
                </div>
                <div className={styles.reconNote}>Dues & registration inflows</div>
              </div>

              <div className={styles.reconCard}>
                <div className={styles.reconLabel}>
                  <span>(-) Total</span>
                  <span>Disbursements</span>
                </div>
                <div className={styles.reconValueNegative}>
                  {formatCurrency(report.totalDisbursements)}
                </div>
                <div className={styles.reconNote}>Operational expenses</div>
              </div>

              <div className={styles.reconCard}>
                <div className={styles.reconLabel}>
                  <span>Net Operating</span>
                  <span>{(report.netOperatingSurplus ?? 0) >= 0 ? 'Surplus' : 'Deficit'}</span>
                </div>
                <div
                  className={
                    (report.netOperatingSurplus ?? 0) >= 0
                      ? styles.reconValuePositive
                      : styles.reconValueNegative
                  }
                >
                  {formatCurrency(report.netOperatingSurplus)}
                </div>
                <div className={styles.reconNote}>
                  {(report.netOperatingSurplus ?? 0) >= 0 ? 'Surplus for Year' : 'Deficit for Year'}
                </div>
              </div>

              <div className={styles.reconCardHighlight}>
                <div className={styles.reconLabel}>
                  <span>Ending Cash</span>
                  <span>(Dec 31)</span>
                </div>
                <div className={styles.reconValue}>
                  {formatCurrency(report.endingCashBalance)}
                </div>
                <div className={styles.reconNote}>
                  Carried forward as baseline for {(report.year ?? selectedYear) + 1}
                </div>
              </div>
            </div>
          </div>

          {/* Section 2: Statement of Operating Receipts */}
          <div className={styles.section}>
            <div className={styles.sectionHeader}>
              <DollarOutlined /> 2. Statement of Operating Receipts (Collections)
            </div>

            <div className={styles.tableContainer}>
              <Table<AnnualFeeTypeReceiptDto>
                size="small"
                pagination={false}
                rowKey="feeType"
                dataSource={report.receiptsByFeeType}
                columns={feeTypeColumns}
                summary={() => (
                  <Table.Summary fixed>
                    <Table.Summary.Row className={styles.totalRow}>
                      <Table.Summary.Cell index={0}>Total Cash Receipts</Table.Summary.Cell>
                      <Table.Summary.Cell index={1} align="right">
                        {formatCurrency(
                          report.receiptsByFeeType?.reduce((acc, r) => acc + (r.q1 || 0), 0)
                        )}
                      </Table.Summary.Cell>
                      <Table.Summary.Cell index={2} align="right">
                        {formatCurrency(
                          report.receiptsByFeeType?.reduce((acc, r) => acc + (r.q2 || 0), 0)
                        )}
                      </Table.Summary.Cell>
                      <Table.Summary.Cell index={3} align="right">
                        {formatCurrency(
                          report.receiptsByFeeType?.reduce((acc, r) => acc + (r.q3 || 0), 0)
                        )}
                      </Table.Summary.Cell>
                      <Table.Summary.Cell index={4} align="right">
                        {formatCurrency(
                          report.receiptsByFeeType?.reduce((acc, r) => acc + (r.q4 || 0), 0)
                        )}
                      </Table.Summary.Cell>
                      <Table.Summary.Cell index={5} align="right">
                        <span className={styles.totalPrimary}>
                          {formatCurrency(report.totalReceipts)}
                        </span>
                      </Table.Summary.Cell>
                    </Table.Summary.Row>
                  </Table.Summary>
                )}
              />
            </div>

            <p className={styles.subTableLabel}>
              Receipts by Payment Channel:
            </p>

            <div className={styles.tableContainer}>
              <Table<AnnualMethodReceiptDto>
                size="small"
                pagination={false}
                rowKey="methodOfPayment"
                dataSource={report.receiptsByPaymentMethod}
                columns={methodColumns}
              />
            </div>
          </div>

          {/* Section 3: Dues & Fee Compliance Performance */}
          <div className={styles.section}>
            <div className={styles.sectionHeader}>
              <TeamOutlined /> 3. Dues & Fee Compliance Performance
            </div>

            <div className={styles.tableContainer}>
              <Table<ComplianceRowItem>
                size="small"
                pagination={false}
                rowKey="key"
                dataSource={complianceData}
                columns={complianceColumns}
                summary={() => (
                  <Table.Summary fixed>
                    <Table.Summary.Row className={styles.totalRow}>
                      <Table.Summary.Cell index={0}>
                        <div className={styles.totalDark}>
                          Total (All Fees)
                        </div>
                      </Table.Summary.Cell>
                      <Table.Summary.Cell index={1} />
                      <Table.Summary.Cell index={2} align="right" />
                      <Table.Summary.Cell index={3} align="right">
                        <span className={styles.totalDark}>
                          {formatCurrency(
                            report.complianceSummary?.combinedSummary?.totalExpectedAmount
                          )}
                        </span>
                      </Table.Summary.Cell>
                      <Table.Summary.Cell index={4} align="right">
                        <span className={styles.totalPrimary}>
                          {formatCurrency(
                            report.complianceSummary?.combinedSummary?.totalActualCollected
                          )}
                        </span>
                      </Table.Summary.Cell>
                      <Table.Summary.Cell index={5} align="center">
                        <div className={styles.progressStack}>
                          {renderEfficiencyBadge(
                            report.complianceSummary?.combinedSummary
                              ?.overallCollectionEfficiencyRate
                          )}
                          <Progress
                            percent={Math.min(
                              report.complianceSummary?.combinedSummary
                                ?.overallCollectionEfficiencyRate ?? 0,
                              100
                            )}
                            size="small"
                            strokeColor={
                              (report.complianceSummary?.combinedSummary
                                ?.overallCollectionEfficiencyRate ?? 0) >= 80
                                ? 'var(--primary-color, #1e5631)'
                                : '#f59e0b'
                            }
                            showInfo={false}
                            className={styles.efficiencyProgressBar}
                          />
                        </div>
                      </Table.Summary.Cell>
                      <Table.Summary.Cell index={6} align="right">
                        <div className={styles.overdueSummaryStack}>
                          <span
                            className={
                              (report.complianceSummary?.combinedSummary?.totalOverdueAmount ??
                                0) > 0
                                ? styles.totalDanger
                                : styles.totalDark
                            }
                          >
                            {formatCurrency(
                              report.complianceSummary?.combinedSummary?.totalOverdueAmount
                            )}
                          </span>
                        </div>
                      </Table.Summary.Cell>
                    </Table.Summary.Row>
                  </Table.Summary>
                )}
              />
            </div>
          </div>

          {/* Section 4: Statement of Operating Disbursements */}
          <div className={styles.section}>
            <div className={styles.sectionHeader}>
              <ShoppingCartOutlined /> 4. Statement of Operating Disbursements (Expenditures)
            </div>

            <div className={styles.tableContainer}>
              <Table<AnnualExpenseCategoryDto>
                size="small"
                pagination={false}
                rowKey="category"
                dataSource={report.disbursementsByCategory}
                columns={expenseColumns}
                summary={() => (
                  <Table.Summary fixed>
                    <Table.Summary.Row className={styles.totalRow}>
                      <Table.Summary.Cell index={0}>Total Operating Disbursements</Table.Summary.Cell>
                      <Table.Summary.Cell index={1} align="right">
                        <span className={styles.totalDanger}>
                          {formatCurrency(report.totalDisbursements)}
                        </span>
                      </Table.Summary.Cell>
                      <Table.Summary.Cell index={2} align="right">
                        100.0%
                      </Table.Summary.Cell>
                      <Table.Summary.Cell index={3} />
                    </Table.Summary.Row>
                  </Table.Summary>
                )}
              />
            </div>
          </div>

          {/* Section 5: Certification & Sign-off */}
          <div className={styles.certification}>
            <div className={styles.certText}>
              I hereby certify that this Annual Financial Statement has been prepared in accordance
              with the association's bylaws, records of cash receipts, and documented expenditures,
              and accurately reflects the financial position and closing cash balance for the year ended December 31, {report.year}.
            </div>

            <div className={styles.signatureGrid}>
              <div>
                <div className={styles.signatureBox}>
                  <span>Prepared By: ___________________________ (Treasurer)</span>
                  <span>Date: _______________</span>
                </div>
              </div>

              <div>
                <div className={styles.signatureBox}>
                  <span>Approved By: ___________________________ (President)</span>
                  <span>Date: _______________</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      ) : (
        <div className={styles.promptContainer}>
          <div className={styles.promptIconCircle}>
            <FileSearchOutlined className={styles.promptIcon} />
          </div>
          <h3 className={styles.promptTitle}>Annual Financial Statement</h3>
          <p className={styles.promptSubtitle}>
            Select a fiscal year and click <strong>Generate Report</strong> to compile the official statement of cash receipts, disbursements, and fund balance reconciliations.
          </p>
          <Button
            type="primary"
            icon={<BarChartOutlined />}
            size="large"
            onClick={handleGenerateReport}
            className={styles.promptButton}
          >
            Generate {selectedYear} Report
          </Button>
        </div>
      )}
    </div>
  );
}