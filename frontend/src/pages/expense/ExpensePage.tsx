import { DeleteOutlined, EditOutlined, ShoppingCartOutlined } from '@ant-design/icons';
import { Button, Card, Modal, Space, Table, Typography } from 'antd';
import { ColumnsType } from 'antd/es/table';
import { useMemo, useState } from 'react';
import { addExpense, deleteExpense, getExpense, updateExpense } from '../../apiclient/expenseApi';
import ExpenseModal from '../../component/ExpenseModal';
import { MessageBanner } from '../../component/MessageBanner';
import SearchFilterBar from '../../component/SearchFilterBar.jsx';
import Sidebar from "../../component/Sidebar";
import { expenseSearchFiltersConfig } from '../../constants/expenseSearchFiltersConfig.ts';
import { EXPENSE_CATEGORY } from "../../constants/referenceConstants";
import { Expense, ExpenseSearchParams, MessageType } from '../../constants/types';
import { useNotification } from "../../context/NotificationContext";
import { useReference } from "../../context/ReferenceContext";
import { useApiMessages } from '../../hook/ApiResponseHandler';
import { usePaginatedExpenses } from '../../hook/PaginatedExpenses';

const { Title } = Typography;

export default function ExpensePage() {

    const notify = useNotification();
    const { expenses, meta, loading, fetchExpenses, setExpenses } = usePaginatedExpenses();
    const { globalMessages, handleError, resetMessages } = useApiMessages<any>();

    // search filters
    const [filters, setFilters] = useState<ExpenseSearchParams>({});

    // Modal State
    const [modalOpenInd, setModalOpenInd] = useState(false);
    const [selectedRecord, setSelectedRecord] = useState<any>(null);

    // Ref Data
    const { getReference, toDisplay } = useReference();
    const categories = getReference(EXPENSE_CATEGORY).map(r => ({ value: r.code, label: r.display }));

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

    const openEdit = (record: Expense) => {
        setSelectedRecord(record);
        setModalOpenInd(true);
    };

    const handleDelete = async (id: number, description: string, amount: number) => {
        Modal.confirm({
            title: 'Are you sure you want to delete expense record?',
            content: (
                <div style={{ color: "red", display: 'grid', gridTemplateColumns: '100px auto', rowGap: '6px', lineHeight: '1.6' }} >
                    <div>Amount:</div> <div>${amount.toFixed(2)}</div>
                    <div>Description:</div> <div>{description}</div>
                </div>
            ),
            okText: 'Delete',
            okType: 'danger',
            onOk: async () => {
                try {
                    resetMessages();
                    const resp = await deleteExpense(id);

                    if (resp.globalMessages?.[0]?.type === MessageType.SUCCESS) {
                        notify.success({ message: "Deleted", description: "Expense deleted successfully." });
                    }
                    fetchExpenses(filters); // Refresh
                } catch (e: any) {
                    handleError(e);
                }
            }
        });
    };

    const handleSubmit = async (values: any) => {
        const isAdd = !selectedRecord?.expenseID;
        const resp = isAdd ? await addExpense(values) : await updateExpense(values);

        if (resp.globalMessages?.[0]?.type === MessageType.SUCCESS) {
            notify.success({ message: "Success", description: resp.globalMessages[0].message });

            if (isAdd && resp.responseData) {
                // Fetch the newly created expense details
                const fullExpenseResp = await getExpense(resp.responseData);
                if (fullExpenseResp.responseData) {
                    // Prepend to the list
                    setExpenses(prev => [fullExpenseResp.responseData!, ...prev]);
                }
            } else if (!isAdd) {
                // For updates, we still refresh the list
                fetchExpenses(filters);
            }
        }
        setModalOpenInd(false);
        setSelectedRecord(null);
        resetMessages();
    };

    const columns: ColumnsType<Expense> = [
        {
            title: 'Category',
            dataIndex: 'category',
            key: 'category',
            render: (code: string) => toDisplay(EXPENSE_CATEGORY, code)
        },
        { title: 'Description', dataIndex: 'description', key: 'description' },
        { title: 'Amount', dataIndex: 'amount', key: 'amount', render: (val: number) => `$${val?.toFixed(2) ?? '0.00'}` },
        { title: 'Date', dataIndex: 'dateOfExpense', key: 'dateOfExpense' },
        {
            title: 'Action',
            key: 'action',
            render: (_, record) => (
                <Space>
                    <Button icon={<EditOutlined />} onClick={() => openEdit(record)} />
                    <Button icon={<DeleteOutlined />} danger onClick={() => handleDelete(record.expenseID, record.description, record.amount)} />
                </Space>
            ),
        },
    ];

    // Dynamic search config
    const searchConfig = useMemo(() => {
        return expenseSearchFiltersConfig.map(filter => {
            if (filter.name === 'category') {
                return { ...filter, options: categories };
            }
            return filter;
        });
    }, [categories]);

    return (
        <div className="dashboard-layout">
            <Sidebar />
            <main className="content fade-in">
                <div style={{ padding: 24 }}>
                    <div className="page-header">
                        <Title level={2} className="page-title">
                            <ShoppingCartOutlined /> Expenses
                        </Title>
                    </div>

                    <SearchFilterBar config={searchConfig as any} filters={filters} onChange={setFilters} onSearch={handleSearch} onAdd={openAdd} />

                    {globalMessages && <MessageBanner messages={globalMessages} />}

                    <Card className="glass-card" style={{ padding: 0 }}>
                        <Table<Expense>
                            size="small"
                            rowKey="expenseID"
                            dataSource={expenses}
                            columns={columns}
                            loading={loading}
                            pagination={{
                                current: meta ? meta.page + 1 : 1,
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
                                    sortOrder: sortOrder === "ascend" ? "asc" : sortOrder === "descend" ? "desc" : undefined,
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
            </main>
        </div>
    );
};