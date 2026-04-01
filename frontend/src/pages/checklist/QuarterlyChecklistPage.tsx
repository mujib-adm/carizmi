import { FileSearchOutlined } from '@ant-design/icons';
import { Card, Table, Typography } from 'antd';
import { ColumnsType } from 'antd/es/table';
import { useMemo, useState } from 'react';
import { checklistApi } from '../../api/generated/checklist/checklist';
import {
  ChecklistSearchRequestDto,
  MemberQuarterlyRowDto,
  PaginationMeta,
  QuarterCellDto,
  QuarterCellStatus,
  QuarterlyChecklistDto,
} from '../../api/generated/types';
import { MessageBanner } from '../../components/MessageBanner';
import SearchFilterBar from '../../components/SearchFilterBar';
import { checklistSearchFiltersConfig } from '../../config/checklistSearchFiltersConfig';
import { useApiMessages } from '../../hooks/useApiMessages';
import styles from '../../styles/pages/QuarterlyChecklist.module.css';

const { Title } = Typography;

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

  const fetchChecklist = async (request: ChecklistSearchRequestDto = {}) => {
    setLoading(true);
    resetMessages();
    setData(null);
    setMeta(null);
    try {
      const resp = await checklistApi.getQuarterlyChecklist({ ...filters, ...request });
      setData(resp.responseData ?? null);
      setMeta(resp.meta ?? null);
    } catch (err: any) {
      handleError(err);
    } finally {
      setLoading(false);
    }
  };

  const handleSearch = () => {
    fetchChecklist({ page: 0, size: meta?.pageSize ?? 10 });
  };

  // Summary calculations
  const summary = useMemo(() => {
    if (!data?.rows?.length) return null;
    const totalPaid = data.rows.reduce((sum, r) => sum + (r.totalPaid ?? 0), 0);
    const totalBalance = data.rows.reduce((sum, r) => sum + (r.balance ?? 0), 0);
    return { totalPaid, totalBalance };
  }, [data]);

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
        onChange={setFilters}
        onSearch={handleSearch}
        onAdd={undefined}
      />

      {globalMessages && <MessageBanner messages={globalMessages} />}

      <Card className="glass-card" style={{ padding: 0 }}>
        <Table<MemberQuarterlyRowDto>
          scroll={{ x: 'max-content' }}
          size="small"
          rowKey="memberID"
          dataSource={data?.rows ?? []}
          columns={columns}
          loading={loading}
          pagination={{
            current: (meta?.page ?? 0) + 1,
            pageSize: meta?.pageSize ?? 10,
            total: meta?.totalRecords ?? 0,
            showSizeChanger: true,
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
                    <strong>Total ({meta?.totalRecords ?? data?.rows?.length ?? 0} members)</strong>
                  </Table.Summary.Cell>
                  {[1, 2, 3, 4].map((q) => (
                    <Table.Summary.Cell key={q} index={q + 1} align="center">
                      —
                    </Table.Summary.Cell>
                  ))}
                  <Table.Summary.Cell index={7} align="right">
                    <strong>${summary.totalPaid.toFixed(2)}</strong>
                  </Table.Summary.Cell>
                  <Table.Summary.Cell index={8} align="right">
                    <span
                      className={summary.totalBalance > 0 ? styles.balanceDue : styles.balanceZero}
                    >
                      <strong>${summary.totalBalance.toFixed(2)}</strong>
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