import { DatabaseOutlined } from '@ant-design/icons';
import { Card, Table, Tag, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useEffect, useState } from 'react';
import { MessageBanner } from '../../../components/MessageBanner';
import SearchFilterBar from '../../../components/SearchFilterBar';
import { referenceSearchFiltersConfig } from '../config/referenceSearchFiltersConfig';
import { ReferenceDto, ReferenceSearchRequestDto } from '../../../api/generated/types';
import { useApiMessages } from '../../../hooks/useApiMessages';
import { usePaginatedReferences } from '../hooks/usePaginatedReferences';

const { Title } = Typography;

export default function ReferencePage() {
  const { references, meta, loading, fetchReferences } = usePaginatedReferences();
  const { globalMessages, handleError, resetMessages } = useApiMessages<any>();

  // search filters
  const [filters, setFilters] = useState<ReferenceSearchRequestDto>({});

  // Initial load
  useEffect(() => {
    fetchReferences().catch(handleError);
  }, [fetchReferences, handleError]);

  const handleSearch = async () => {
    resetMessages();
    try {
      await fetchReferences({ ...filters, page: 0, size: meta?.pageSize ?? 10 });
    } catch (e: any) {
      handleError(e);
    }
  };

  const columns: ColumnsType<ReferenceDto> = [
    { title: 'Reference Name', dataIndex: 'referenceName', key: 'referenceName' },
    { title: 'Code', dataIndex: 'referenceCode', key: 'referenceCode' },
    { title: 'Display', dataIndex: 'referenceDisplay', key: 'referenceDisplay' },
    {
      title: 'Active',
      dataIndex: 'active',
      key: 'active',
      render: (act: boolean) => <Tag color={act ? 'green' : 'red'}>{act ? 'Yes' : 'No'}</Tag>,
    },
  ];

  return (
    <div>
      <div className="page-header">
        <Title level={2} className="page-title">
          <DatabaseOutlined /> References
        </Title>
      </div>

      <SearchFilterBar
        config={referenceSearchFiltersConfig as any}
        filters={filters}
        onChange={setFilters as any}
        onSearch={handleSearch}
        onAdd={undefined as any}
      />

      {globalMessages && <MessageBanner messages={globalMessages} />}

      <Card className="glass-card" style={{ padding: 0 }}>
        <Table<ReferenceDto>
          scroll={{ x: 'max-content' }}
          size="small"
          rowKey="referenceID"
          dataSource={references}
          columns={columns}
          loading={loading}
          pagination={{
            current: (meta?.page ?? 0) + 1,
            pageSize: meta?.pageSize ?? 10,
            total: meta?.totalRecords ?? 0,
            showSizeChanger: true,
          }}
          onChange={(pagination, _filters, sorter) => {
            const sortField = Array.isArray(sorter) ? sorter[0].field : sorter.field;
            const sortOrder = Array.isArray(sorter) ? sorter[0].order : sorter.order;

            resetMessages();
            fetchReferences({
              ...filters,
              page: (pagination.current ?? 1) - 1,
              size: pagination.pageSize ?? 10,
              sortField: sortField as string,
              sortOrder:
                sortOrder === 'ascend' ? 'asc' : sortOrder === 'descend' ? 'desc' : undefined,
            }).catch(handleError);
          }}
        />
      </Card>
    </div>
  );
}