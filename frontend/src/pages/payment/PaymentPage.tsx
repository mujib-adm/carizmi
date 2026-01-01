import { DeleteOutlined, DollarOutlined, EditOutlined } from '@ant-design/icons';
import { Button, Card, Modal, Space, Table, Typography } from 'antd';
import { ColumnsType } from 'antd/es/table';
import { useMemo, useState } from 'react';
import { addPayment, deletePayment, getPayment, updatePayment } from '../../apiclient/paymentApi';
import { MessageBanner } from '../../component/MessageBanner';
import PaymentModal from '../../component/PaymentModal';
import SearchFilterBar from '../../component/SearchFilterBar.jsx';
import Sidebar from "../../component/Sidebar";
import { paymentSearchFiltersConfig } from '../../constants/paymentSearchFiltersConfig.ts';
import { FEE_TYPE, PAYMENT_METHOD } from "../../constants/referenceConstants";
import { MessageType, Payment, PaymentSearchParams } from '../../constants/types';
import { useNotification } from "../../context/NotificationContext";
import { useReference } from "../../context/ReferenceContext";
import { useApiMessages } from '../../hook/ApiResponseHandler';
import { usePaginatedPayments } from '../../hook/PaginatedPayments';

const { Title } = Typography;

export default function PaymentPage() {

    const notify = useNotification();
    const { payments, meta, loading, fetchPayments, setPayments } = usePaginatedPayments();
    const { globalMessages, handleError, resetMessages } = useApiMessages<any>();

    // search filters
    const [filters, setFilters] = useState<PaymentSearchParams>({});

    // Modal State
    const [modalOpenInd, setModalOpenInd] = useState(false);
    const [selectedRecord, setSelectedRecord] = useState<any>(null);

    // Ref Data
    const { getReference, toDisplay } = useReference();
    const feeTypes = getReference(FEE_TYPE).map(r => ({ value: r.code, label: r.display }));
    const paymentMethods = getReference(PAYMENT_METHOD).map(r => ({ value: r.code, label: r.display }));

    const handleSearch = async () => {
        resetMessages();
        try {
            await fetchPayments({ ...filters, page: 0, size: meta?.pageSize ?? 10 });
        } catch (e: any) {
            handleError(e);
        }
    };

    const openAdd = () => {
        setSelectedRecord(null);
        setModalOpenInd(true);
    };

    const openEdit = (record: Payment) => {
        setSelectedRecord(record);
        setModalOpenInd(true);
    };

    const handleDelete = async (id: number, memberName: string, payment: string) => {
        Modal.confirm({
            title: 'Are you sure you want to delete payment record?',
            content: (
                <div style={{ color: "red", display: 'grid', gridTemplateColumns: '100px auto', rowGap: '6px', lineHeight: '1.6' }} >
                    <div>Payment:</div> <div>{payment}</div>
                    <div>Member:</div> <div>{memberName}</div>
                </div>
            ),
            okText: 'Delete',
            okType: 'danger',
            onOk: async () => {
                try {
                    resetMessages();
                    const resp = await deletePayment(id);

                    if (resp.globalMessages?.[0]?.type === MessageType.SUCCESS) {
                        notify.success({ message: "Deleted", description: "Payment deleted successfully." });
                    }
                    fetchPayments(filters); // Refresh
                } catch (e: any) {
                    handleError(e);
                }
            }
        });
    };

    const handleSubmit = async (values: any) => {
        const isAdd = !selectedRecord?.paymentID;
        const resp = isAdd ? await addPayment(values) : await updatePayment(values);

        if (resp.globalMessages?.[0]?.type === MessageType.SUCCESS) {
            notify.success({ message: "Success", description: resp.globalMessages[0].message });

            if (isAdd && resp.responseData) {
                // Fetch the newly created payment details
                const fullPaymentResp = await getPayment(resp.responseData);
                if (fullPaymentResp.responseData) {
                    // Prepend to the list
                    setPayments(prev => [fullPaymentResp.responseData!, ...prev]);
                }
            } else if (!isAdd) {
                // For updates, we still refresh the list to ensure sorting/filtering logic
                fetchPayments(filters);
            }
        }
        setModalOpenInd(false);
        setSelectedRecord(null);
        resetMessages();
    };

    const columns: ColumnsType<Payment> = [
        { title: 'Member', dataIndex: 'memberFullName', key: 'memberFullName' },
        {
            title: 'Fee Type',
            dataIndex: 'feeType',
            key: 'feeType',
            render: (code: string) => toDisplay(FEE_TYPE, code)
        },
        { title: 'Amount', dataIndex: 'amount', key: 'amount', render: (val: number) => `$${val?.toFixed(2) ?? '0.00'}` },
        { title: 'Date', dataIndex: 'dateReceived', key: 'dateReceived' },
        { title: 'Method', dataIndex: 'methodOfPayment', key: 'methodOfPayment', render: (code: string) => toDisplay(PAYMENT_METHOD, code) },
        { title: 'Period', key: 'period', render: (_, record) => record.year && record.quarter ? `${record.year}-Q${record.quarter}` : '' },
        {
            title: 'Action',
            key: 'action',
            render: (_, record) => (
                <Space>
                    <Button icon={<EditOutlined />} onClick={() => openEdit(record)} />
                    <Button icon={<DeleteOutlined />} danger onClick={() => handleDelete(record.paymentID, record.memberFullName, `${record.feeType} ($${record.amount})`)} />
                </Space>
            ),
        },
    ];

    // Dynamic search config for fee types (Reference data)
    const searchConfig = useMemo(() => {
        return paymentSearchFiltersConfig.map(filter => {
            if (filter.name === 'feeType') {
                return { ...filter, options: feeTypes };
            }
            return filter;
        });
    }, [feeTypes]);

    return (
        <div className="dashboard-layout">
            <Sidebar />
            <main className="content fade-in">
                <div style={{ padding: 24 }}>
                    <div className="page-header">
                        <Title level={2} className="page-title">
                            <DollarOutlined /> Payments
                        </Title>
                    </div>

                    <SearchFilterBar config={searchConfig as any} filters={filters} onChange={setFilters} onSearch={handleSearch} onAdd={openAdd} />

                    {globalMessages && <MessageBanner messages={globalMessages} />}

                    <Card className="glass-card" style={{ padding: 0 }}>
                        <Table<Payment>
                            size="small"
                            rowKey="paymentID"
                            dataSource={payments}
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
                                fetchPayments({
                                    ...filters, // use the state-level search filters
                                    page: (pagination.current ?? 1) - 1,
                                    size: pagination.pageSize ?? 10,
                                    sortField: sortField as string,
                                    sortOrder: sortOrder === "ascend" ? "asc" : sortOrder === "descend" ? "desc" : undefined,
                                }).catch(handleError);
                            }}
                        />
                    </Card>

                    <PaymentModal
                        open={modalOpenInd}
                        onCancel={() => setModalOpenInd(false)}
                        onSubmit={handleSubmit}
                        initialValues={selectedRecord}
                        feeTypes={feeTypes}
                        paymentMethods={paymentMethods}
                    />
                </div>
            </main>
        </div>
    );
};