import { DeleteOutlined, EditOutlined } from '@ant-design/icons';
import { Button, Card, Modal, Space, Table, Typography } from "antd";
import type { ColumnsType } from "antd/es/table";
import { useEffect, useMemo, useState } from "react";
import { addMember, deleteMember, updateMember } from "../../apiclient/memberApi";
import { MemberModal } from "../../component/MemberModal";
import { MessageBanner } from "../../component/MessageBanner";
import SearchFilterBar from "../../component/SearchFilterBar";
import Sidebar from "../../component/Sidebar";
import { memberSearchFiltersConfig } from "../../constants/memberSearchFiltersConfig";
import { MEMBER_STATUS } from "../../constants/referenceConstants";
import { Member, MemberRequestDto, MemberSearchParams, MessageType } from "../../constants/types";
import { useNotification } from "../../context/NotificationContext";
import { useReference } from "../../context/ReferenceContext";
import { useApiMessages } from "../../hook/ApiResponseHandler";
import { usePaginatedMembers } from "../../hook/PaginatedMembers";

const { Title } = Typography;

export default function MemberPage() {

    const notify = useNotification();
    const { members, meta, loading, fetchMembers } = usePaginatedMembers();
    const { globalMessages, handleError, resetMessages } = useApiMessages<any>();

    // search filters
    const [filters, setFilters] = useState<MemberSearchParams>({});

    // modal state
    const [modalOpenInd, setModalOpenInd] = useState(false);
    const [selectedRecord, setSelectedRecord] = useState<Member | null>(null);

    const { getReference, toDisplay } = useReference();
    const statusOptions = useMemo(
        () => getReference(MEMBER_STATUS).map(r => ({ value: r.code, label: r.display })),
        [getReference]
    );

    // Initial load
    useEffect(() => {
        fetchMembers().catch(handleError);
    }, []);

    const handleSearch = async () => {
        resetMessages();
        try {
            await fetchMembers({ ...filters, page: 0, size: meta?.pageSize ?? 10 });
        } catch (e: any) {
            console.error("Error searching members: -> ", e);
            handleError(e);
        }
    };

    const openAdd = () => {
        setSelectedRecord(null);
        setModalOpenInd(true);
    };

    const openEdit = (record: Member) => {
        setSelectedRecord(record);
        setModalOpenInd(true);
    };

    const handleDelete = async (id: number, name: string) => {
        Modal.confirm({
            title: 'Are you sure you want to delete member record?',
            content: (
                <div style={{ color: "red", display: 'grid', gridTemplateColumns: '100px auto', rowGap: '6px', lineHeight: '1.6' }} >
                    <div>Member ID:</div> <div>{id}</div> <div>Name:</div> <div>{name}</div>
                </div>
            ),
            okText: 'Delete',
            okType: 'danger',
            onOk: async () => {
                try {
                    resetMessages();
                    const resp = await deleteMember(id);

                    if (resp.globalMessages?.[0]?.type === MessageType.SUCCESS) {
                        notify.success({ message: "Deleted", description: "Member deleted successfully." });
                        fetchMembers(filters); // Refresh
                    }
                } catch (e: any) {
                    handleError(e);
                }
            }
        });
    };

    const handleSubmit = async (values: MemberRequestDto) => {
        const resp = selectedRecord?.memberID ? await updateMember(values) : await addMember(values);

        if (resp.globalMessages?.[0]?.type === MessageType.SUCCESS) {
            notify.success({ message: "Success", description: resp.globalMessages[0].message });
        }
        setModalOpenInd(false);
        setSelectedRecord(null);
        resetMessages();
        fetchMembers(filters);
    };

    const columns: ColumnsType<Member> = [
        { title: "Member ID", dataIndex: "memberID", key: "memberID", sorter: true, width: 140 },
        { title: "First Name", dataIndex: "firstName", key: "firstName", sorter: true },
        { title: "Last Name", dataIndex: "lastName", key: "lastName", sorter: true },
        { title: "Phone", dataIndex: "phone", key: "phone", width: 120 },
        { title: "Email", dataIndex: "email", key: "email", width: 150 },
        { title: "Status", dataIndex: "status", key: "status", width: 120, render: (code: string) => toDisplay(MEMBER_STATUS, code) },
        {
            title: 'Action',
            key: 'action',
            render: (_, record) => (
                <Space>
                    <Button icon={<EditOutlined />} onClick={() => openEdit(record)} />
                    <Button icon={<DeleteOutlined />} danger onClick={() => handleDelete(record.memberID, `${record.firstName} ${record.lastName}`)} />
                </Space>
            ),
        },
    ];

    return (
        <div className="dashboard">
            <Sidebar />
            <main className="content">
                <div style={{ padding: 24 }}>
                    <div className="content-title">
                        <Title level={3}>Members</Title>
                    </div>

                    <Card style={{ marginBottom: 16 }}>
                        <SearchFilterBar config={memberSearchFiltersConfig as any} filters={filters} onChange={setFilters} onSearch={handleSearch} onAdd={openAdd} />
                    </Card>

                    {globalMessages && <MessageBanner messages={globalMessages} />}

                    <Table<Member>
                        size="small"
                        rowKey="memberID"
                        columns={columns}
                        dataSource={members}
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
                            fetchMembers({
                                ...filters, // your current search filters
                                page: (pagination.current ?? 1) - 1,
                                size: pagination.pageSize ?? 10,
                                sortField: sortField as string,
                                sortOrder: sortOrder === "ascend" ? "asc" : sortOrder === "descend" ? "desc" : undefined,
                            }).catch(handleError);
                        }}
                    />

                    <MemberModal
                        open={modalOpenInd}
                        onCancel={() => setModalOpenInd(false)}
                        onSubmit={handleSubmit}
                        initial={selectedRecord}
                        statusOptions={statusOptions}
                    />
                </div >
            </main >
        </div >
    );
}