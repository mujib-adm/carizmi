import { FileSearchOutlined } from '@ant-design/icons';
import { Card, Checkbox, Form, Grid, Table, Tooltip, Typography } from 'antd';
import { ColumnsType } from 'antd/es/table';
import { useState } from 'react';
import { checklistApi } from '../../../api/generated/checklist/checklist';
import {
  ChecklistSearchRequestDto,
  MemberQuarterlyRowDto,
  PaginationMeta,
  QuarterCellDto,
  QuarterCellStatus,
  QuarterlyChecklistDto,
} from '../../../api/generated/types';
import { MessageBanner } from '../../../components/MessageBanner';
import SearchFilterBar from '../../../components/SearchFilterBar';
import { checklistSearchFiltersConfig } from '../config/checklistSearchFiltersConfig';
import { useApiMessages } from '../../../hooks/useApiMessages';
import styles from '../../../styles/pages/QuarterlyChecklist.module.css';

const { Title } = Typography;
const { useBreakpoint } = Grid;

function renderQuarterCell(cell: QuarterCellDto | undefined) {
  if (!cell) return null;

  switch (cell.status) {
    case QuarterCellStatus.PAID:
      return <span className={`${styles.statusCell} ${styles.statusPaid}`}>✅</span>;
    case QuarterCellStatus.UNPAID:
      return <span className={`${styles.statusCell} ${styles.statusUnpaid}`}>❌</span>;
    case QuarterCellStatus.NOT_APPLICABLE:
      return <span className={`${styles.statusCell} ${styles.statusNA}`}>N/A</span>;
    case QuarterCellStatus.FUTURE:
      return <span className={`${styles.statusCell} ${styles.statusFuture}`}>—</span>;
    default:
      return null;
  }
}

export default function QuarterlyChecklistPage() {
  const [filters, setFilters] = useState<ChecklistSearchRequestDto>({
    year: new Date().getFullYear(),
  });
  const [data, setData] = useState<QuarterlyChecklistDto | null>(null);
  const [meta, setMeta] = useState<PaginationMeta | null>(null);
  const [loading, setLoading] = useState(false);
  const { globalMessages, handleError, resetMessages } = useApiMessages<any>();
  const screens = useBreakpoint();
  const isMobile = !screens.md;

  const now = new Date();
  const currentYear = now.getFullYear();
  const currentMonth = now.getMonth(); // 0 to 11
  const currentQuarter = Math.floor(currentMonth / 3) + 1; // 1 to 4

  const selectedYear = filters.year ?? currentYear;

  const isQuarterFuture = (q: number) => {
    if (selectedYear > currentYear) return true;
    if (selectedYear === currentYear) return q > currentQuarter;
    return false;
  };

  const fetchChecklist = async (request: ChecklistSearchRequestDto = {}) => {
    setLoading(true);
    resetMessages();
    try {
      const mergedRequest = { ...filters, ...request };
      const resp = await checklistApi.getQuarterlyChecklist(mergedRequest);
      setData(resp.responseData ?? null);
      setMeta(resp.meta ?? null);
    } catch (err: any) {
      handleError(err);
      setData(null);
      setMeta(null);
    } finally {
      setLoading(false);
    }
  };

  const handleSearch = () => {
    fetchChecklist({ page: 0, size: meta?.pageSize ?? 10 });
  };

  const handleQuarterToggle = (q: number, checked: boolean) => {
    const current = filters.unpaidQuarters ?? [];
    const updated = checked
      ? [...current, q].sort((a, b) => a - b)
      : current.filter((item) => item !== q);

    setFilters((prev) => ({
      ...prev,
      unpaidQuarters: updated.length > 0 ? updated : undefined,
    }));
  };

  const handleClearQuarterFilters = () => {
    setFilters((prev) => ({
      ...prev,
      unpaidQuarters: undefined,
    }));
  };

  const handleFilterChange = (updated: ChecklistSearchRequestDto) => {
    const newYear = updated.year ?? currentYear;
    const isFutureForNewYear = (q: number) => {
      if (newYear > currentYear) return true;
      if (newYear === currentYear) return q > currentQuarter;
      return false;
    };
    // Auto-uncheck quarters that become future when year changes
    const cleanedQuarters = (filters.unpaidQuarters ?? []).filter((q) => !isFutureForNewYear(q));
    setFilters({
      ...updated,
      unpaidQuarters: cleanedQuarters.length > 0 ? cleanedQuarters : undefined,
    });
  };

  const summary = data?.summary;

  const columns: ColumnsType<MemberQuarterlyRowDto> = [
    {
      title: 'ID',
      dataIndex: 'memberID',
      key: 'memberID',
      width: 70,
    },
    {
      title: 'Name',
      dataIndex: 'memberName',
      key: 'memberName',
      width: 160,
    },
    ...[1, 2, 3, 4].map((q) => ({
      title: `Q${q}`,
      key: `q${q}`,
      width: 60,
      align: 'center' as const,
      render: (_: any, record: MemberQuarterlyRowDto) => {
        const cell = record.quarters?.find((c) => c.quarter === q);
        return renderQuarterCell(cell);
      },
    })),
    {
      title: 'Total Paid',
      dataIndex: 'totalPaid',
      key: 'totalPaid',
      width: 110,
      align: 'right' as const,
      render: (val: number) => `$${(val ?? 0).toFixed(2)}`,
    },
    {
      title: 'Balance',
      dataIndex: 'balance',
      key: 'balance',
      width: 100,
      align: 'right' as const,
      render: (val: number) => (
        <span className={val > 0 ? styles.balanceDue : styles.balanceZero}>
          ${(val ?? 0).toFixed(2)}
        </span>
      ),
    },
  ];

  return (
    <div>
      <div className="page-header">
        <Title level={2} className="page-title">
          <FileSearchOutlined /> Quarterly Fee Checklist
        </Title>
      </div>

      <SearchFilterBar
        config={checklistSearchFiltersConfig as any}
        filters={filters}
        onChange={handleFilterChange}
        onSearch={handleSearch}
        onAdd={undefined}
      >
        <Form.Item
          label={
            <span style={{ fontWeight: 600, color: 'var(--otherColor2)' }}>
              Quarters (Unpaid)
            </span>
          }
          style={{ marginBottom: 8, marginRight: 16 }}
        >
          <div className={styles.quarterCheckboxGroup}>
            {[1, 2, 3, 4].map((q) => {
              const isFuture = isQuarterFuture(q);
              const isChecked = !isFuture && (filters.unpaidQuarters?.includes(q) ?? false);
              const content = (
                <label
                  key={q}
                  className={`${styles.quarterCheckboxItem} ${
                    isChecked ? styles.quarterCheckboxChecked : ''
                  } ${isFuture ? styles.quarterCheckboxDisabled : ''}`}
                >
                  <Checkbox
                    checked={isChecked}
                    disabled={isFuture}
                    onChange={(e) => handleQuarterToggle(q, e.target.checked)}
                    style={{ marginRight: 6 }}
                  />
                  <span>Q{q}</span>
                  {isFuture && (
                    <span style={{ fontSize: '0.75rem', marginLeft: 4, opacity: 0.65 }}>(Future)</span>
                  )}
                </label>
              );

              return isFuture ? (
                <Tooltip key={q} title={`Quarter ${q} of ${selectedYear} is in the future`}>
                  {content}
                </Tooltip>
              ) : (
                content
              );
            })}

            {filters.unpaidQuarters && filters.unpaidQuarters.length > 0 && (
              <button
                type="button"
                className={styles.clearQuartersBtn}
                onClick={handleClearQuarterFilters}
                title="Clear selected quarters"
              >
                Clear
              </button>
            )}
          </div>
        </Form.Item>
      </SearchFilterBar>

      {globalMessages && <MessageBanner messages={globalMessages} />}

      <Card className="glass-card" style={{ padding: 0 }}>
        <Table<MemberQuarterlyRowDto>
          scroll={{ x: true }}
          size="small"
          rowKey="memberID"
          dataSource={data?.rows ?? []}
          columns={columns}
          loading={loading}
          pagination={{
            current: (meta?.page ?? 0) + 1,
            pageSize: meta?.pageSize ?? 10,
            total: meta?.totalRecords ?? 0,
            showSizeChanger: !isMobile,
            simple: isMobile,
          }}
          onChange={(pagination) => {
            resetMessages();
            fetchChecklist({
              page: (pagination.current ?? 1) - 1,
              size: pagination.pageSize ?? 10,
            });
          }}
          summary={() =>
            summary ? (
              <Table.Summary fixed>
                <Table.Summary.Row className={styles.summaryRow}>
                  <Table.Summary.Cell index={0} colSpan={2}>
                    Total ({meta?.totalRecords ?? data?.rows?.length ?? 0} members)
                  </Table.Summary.Cell>
                  {summary.quarterSummaries?.map((qs, idx) => (
                    <Table.Summary.Cell key={idx} index={idx + 2} align="center">
                      {qs.future ? (
                        '—'
                      ) : (
                        <span className={styles.quarterSummary}>
                          <span className={styles.summaryPaid}>{qs.paidCount}</span>
                          <span className={styles.summaryDivider}>/</span>
                          <span className={styles.summaryUnpaid}>{qs.unpaidCount}</span>
                        </span>
                      )}
                    </Table.Summary.Cell>
                  ))}
                  <Table.Summary.Cell index={6} align="right">
                    ${(summary.totalPaid ?? 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                  </Table.Summary.Cell>
                  <Table.Summary.Cell index={7} align="right">
                    <span
                      className={(summary.totalBalance ?? 0) > 0 ? styles.balanceDue : styles.balanceZero}
                    >
                      ${(summary.totalBalance ?? 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                    </span>
                  </Table.Summary.Cell>
                </Table.Summary.Row>
              </Table.Summary>
            ) : undefined
          }
        />
      </Card>
    </div>
  );
}