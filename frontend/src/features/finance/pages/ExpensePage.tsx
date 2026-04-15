import { DeleteOutlined, EditOutlined, ShoppingCartOutlined } from '@ant-design/icons';
import { Button, Card, Modal, Space, Table, Typography } from 'antd';
import { ColumnsType } from 'antd/es/table';
import { useMemo, useState } from 'react';
import { expensesApi } from '../../../api/generated/expenses/expenses';
import ExpenseModal from '../modals/ExpenseModal';
import { MessageBanner } from '../../../components/MessageBanner';
import SearchFilterBar from '../../../components/SearchFilterBar';
import { expenseSearchFiltersConfig } from '../config/expenseSearchFiltersConfig';
import { ReferenceConstants } from '../../../constants/ReferenceConstants';
import {
  ExpenseDto,
  ExpenseSearchRequestDto,
  GlobalResponse,
  MessageType,
} from '../../../api/generated/types';
import { useNotification } from '../../../hooks/useNotification';
import { useReference } from '../../../hooks/useReference';
import { useApiMessages } from '../../../hooks/useApiMessages';
import { usePaginatedExpenses } from '../hooks/usePaginatedExpenses';
import { useAuthorization } from '../../../hooks/useAuthorization';

const { Title } = Typography;

export default function ExpensePage() {
  const notify = useNotification();
  const { expenses, meta, loading, fetchExpenses, setExpenses } = usePaginatedExpenses();
  const { globalMessages, handleResponse, handleError, resetMessages } = useApiMessages<any>();

  // search filters
  const [filters, setFilters] = useState<ExpenseSearchRequestDto>({});

  // Modal State
  const [modalOpenInd, setModalOpenInd] = useState(false);
  const [selectedRecord, setSelectedRecord] = useState<ExpenseDto | null>(null);

  const { canWrite } = useAuthorization();

  // Ref Data
  const { getReference, toDisplay } = useReference();
  const categories = getReference(ReferenceConstants.EXPENSE_CATEGORY.NAME).map((r) => ({
    value: r.referenceCode || '',
    label: r.referenceDisplay || '',
  }));

  const handleSearch = async () => {
    resetMessages();
    try {
      await fetchExpenses({ ...filters, page: 0, size: meta?.pageSize ?? 10 });
    } catch (e: any) {
      handleError(e);
    }
  };

  const openAdd = () => {
    setSelectedRecord(null);
    setModalOpenInd(true);
  };

  const openEdit = (record: ExpenseDto) => {
    setSelectedRecord(record);
    setModalOpenInd(true);
  };

  const handleDelete = async (record: ExpenseDto) => {
    const categoryDisplay = record.category
      ? toDisplay(ReferenceConstants.EXPENSE_CATEGORY.NAME, record.category)
      : record.category;

    Modal.confirm({
      title: 'Are you sure you want to delete expense record?',
      content: (
        <div
          className="danger-text"
          style={{
            display: 'grid',
            gridTemplateColumns: 'min-content auto',
            columnGap: '12px',
            rowGap: '6px',
            lineHeight: '1.6',
          }}
        >
          <div style={{ whiteSpace: 'nowrap', fontWeight: 600 }}>Category:</div>
          <div>{categoryDisplay}</div>
          <div style={{ whiteSpace: 'nowrap', fontWeight: 600 }}>Description:</div>
          <div>{record.description}</div>
          <div style={{ whiteSpace: 'nowrap', fontWeight: 600 }}>Amount:</div>
          <div>${record.amount?.toFixed(2) || '0.00'}</div>
        </div>
      ),
      okText: 'Delete',
      okType: 'danger',
      onOk: async () => {
        try {
          resetMessages();
          if (record.expenseID) {
            const resp = await expensesApi.deleteExpense(record.expenseID);

            if (resp.globalMessages?.[0]?.type === MessageType.SUCCESS) {
              notify.success({ message: 'Deleted', description: 'Expense deleted successfully.' });
            } else {
              handleResponse(resp);
            }
          }
          fetchExpenses(filters); // Refresh
        } catch (e: any) {
          handleError(e);
        }
      },
    });
  };

  const handleSubmit = async (values: ExpenseDto) => {
    const isAdd = !selectedRecord?.expenseID;
    const resp = isAdd
      ? await expensesApi.addExpense(values)
      : await expensesApi.updateExpense(values);

    if (resp.globalMessages?.[0]?.type === MessageType.SUCCESS) {
      notify.success({ message: 'Success', description: resp.globalMessages[0].message });

      if (isAdd && resp.responseData) {
        // Fetch the newly created expense details
        const fullExpenseResp = await expensesApi.getExpense(resp.responseData);
        if (fullExpenseResp.responseData) {
          // Prepend to the list
          setExpenses((prev) => [fullExpenseResp.responseData!, ...prev]);
        }
      } else if (!isAdd) {
        // For updates, we still refresh the list
        fetchExpenses(filters);
      }
    } else {
      handleResponse(resp as GlobalResponse<unknown>);
    }
    setModalOpenInd(false);
    setSelectedRecord(null);
    resetMessages();
  };

  const columns: ColumnsType<ExpenseDto> = [
    {
      title: 'Category',
      dataIndex: 'category',
      key: 'category',
      render: (code: string) => toDisplay(ReferenceConstants.EXPENSE_CATEGORY.NAME, code),
    },
    { title: 'Description', dataIndex: 'description', key: 'description' },
    {
      title: 'Amount',
      dataIndex: 'amount',
      key: 'amount',
      render: (val: number) => `$${val?.toFixed(2) ?? '0.00'}`,
    },
    { title: 'Date', dataIndex: 'dateOfExpense', key: 'dateOfExpense' },
    ...(canWrite
      ? [
          {
            title: 'Action',
            key: 'action',
            render: (_: any, record: ExpenseDto) => (
              <Space>
                <Button icon={<EditOutlined />} onClick={() => openEdit(record)} />
                <Button icon={<DeleteOutlined />} danger onClick={() => handleDelete(record)} />
              </Space>
            ),
          },
        ]
      : []),
  ];

  // Dynamic search config
  const searchConfig = useMemo(() => {
    return expenseSearchFiltersConfig.map((filter) => {
      if (filter.name === 'category') {
        return { ...filter, options: categories };
      }
      return filter;
    });
  }, [categories]);

  return (
    <div>
      <div className="page-header">
        <Title level={2} className="page-title">
          <ShoppingCartOutlined /> Expenses
        </Title>
      </div>

      <SearchFilterBar
        config={searchConfig as any}
        filters={filters}
        onChange={setFilters}
        onSearch={handleSearch}
        onAdd={canWrite ? openAdd : undefined}
      />

      {globalMessages && <MessageBanner messages={globalMessages} />}

      <Card className="glass-card" style={{ padding: 0 }}>
        <Table<ExpenseDto>
          scroll={{ x: 'max-content' }}
          size="small"
          rowKey="expenseID"
          dataSource={expenses}
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
            fetchExpenses({
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

      <ExpenseModal
        open={modalOpenInd}
        onCancel={() => setModalOpenInd(false)}
        onSubmit={handleSubmit}
        initialValues={selectedRecord}
        categories={categories}
      />
    </div>
  );
}