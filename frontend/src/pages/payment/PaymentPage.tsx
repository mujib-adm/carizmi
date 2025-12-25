import { DeleteOutlined, EditOutlined } from '@ant-design/icons';
import { Button, Card, DatePicker, Modal, Select, Space, Table, Typography } from 'antd';
import { ColumnsType } from 'antd/es/table';
import { useEffect, useState } from 'react';
import { addPayment, deletePayment, updatePayment } from '../../apiclient/paymentApi';
import { getSettingsByKey } from '../../apiclient/systemSettingsApi';
import { MessageBanner } from '../../component/MessageBanner';
import PaymentModal from '../../component/PaymentModal';
import SearchFilterBar from '../../component/SearchFilterBar.jsx';
import Sidebar from "../../component/Sidebar";
import { paymentSearchFiltersConfig } from '../../constants/paymentSearchFiltersConfig.ts';
import { MessageType, Payment, PaymentSearchParams } from '../../constants/types';
import { useNotification } from "../../context/NotificationContext";
import { useApiMessages } from '../../hook/ApiResponseHandler';
import { usePaginatedPayments } from '../../hook/PaginatedPayments';

const { Title } = Typography;
const { Option } = Select;
const { RangePicker } = DatePicker;

export default function PaymentPage() {

    const notify = useNotification();
    const { payments, meta, loading, fetchPayments } = usePaginatedPayments();
    const { globalMessages, handleError, resetMessages } = useApiMessages<any>();

    // search filters
    const [filters, setFilters] = useState<PaymentSearchParams>({});

    // Modal State
    const [modalOpenInd, setModalOpenInd] = useState(false);
    const [selectedRecord, setSelectedRecord] = useState<any>(null);

    // Ref Data
    const [feeTypes, setFeeTypes] = useState<any[]>([]);
    const [paymentMethods, setPaymentMethods] = useState<any[]>([]);

    // Initial load
    useEffect(() => {
        fetchRefData();
        fetchPayments().catch(handleError);
    }, []);

    const fetchRefData = async () => {
        try {
            const feeRes = await getSettingsByKey('feeType');
            const pmRes = await getSettingsByKey('paymentMethod');
            setFeeTypes(feeRes.responseData || []);
            setPaymentMethods(pmRes.responseData || []);
        } catch (e: any) {
            handleError(e);
        }
    };

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
        try {
            const resp = selectedRecord?.paymentID ? await updatePayment(values) : await addPayment(values);

            if (resp.globalMessages?.[0]?.type === MessageType.SUCCESS) {
                notify.success({ message: "Success", description: resp.globalMessages[0].message });
            }
            setModalOpenInd(false);
            setSelectedRecord(null);
            resetMessages();
            fetchPayments(filters);
        } catch (e: any) {
            handleError(e);
        }
    };

    const columns: ColumnsType<Payment> = [
        { title: 'Member', dataIndex: 'memberFullName', key: 'memberFullName' },
        { title: 'Fee Type', dataIndex: 'feeType', key: 'feeType' },
        { title: 'Amount', dataIndex: 'amount', key: 'amount', render: (val: any) => `$${val}` },
        { title: 'Date', dataIndex: 'dateReceived', key: 'dateReceived' },
        { title: 'Method', dataIndex: 'methodOfPayment', key: 'methodOfPayment' },
        {
            title: 'Period',
            key: 'period',
            render: (_, record) => record.year && record.quarter ? `${record.year}-Q${record.quarter}` : ''
        },
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

    return (
        <div className="dashboard">
            <Sidebar />
            <main className="content">
                <div style={{ padding: 24 }}>
                    <div className="member-header">
                        <Title level={3}>Payments</Title>
                    </div>

                    <Card style={{ marginBottom: 16 }}>
                        <SearchFilterBar config={paymentSearchFiltersConfig as any} filters={filters} onChange={setFilters} onSearch={handleSearch} onAdd={openAdd} />
                    </Card>

                    {globalMessages && <MessageBanner messages={globalMessages} />}

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
                        onChange={(pagination, filters, sorter) => {
                            const sortField = Array.isArray(sorter) ? sorter[0].field : sorter.field;
                            const sortOrder = Array.isArray(sorter) ? sorter[0].order : sorter.order;

                            resetMessages();
                            fetchPayments({
                                ...filters, // your current search filters
                                page: (pagination.current ?? 1) - 1,
                                size: pagination.pageSize ?? 10,
                                sortField: sortField as string,
                                sortOrder: sortOrder === "ascend" ? "asc" : sortOrder === "descend" ? "desc" : undefined,
                            }).catch(handleError);
                        }}
                    />

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