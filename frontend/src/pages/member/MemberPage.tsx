import { DeleteOutlined, EditOutlined, EyeOutlined, TeamOutlined } from '@ant-design/icons';
import { Button, Card, Modal, Space, Table, Typography } from "antd";
import type { ColumnsType } from "antd/es/table";
import { useMemo, useState } from "react";
import { useNavigate } from 'react-router-dom';
import { addMember, deleteMember, getMember, updateMember } from "../../apiclient/memberApi";
import { MemberModal } from "../../modals/MemberModal";
import { MessageBanner } from "../../component/MessageBanner";
import SearchFilterBar from "../../component/SearchFilterBar";
import Sidebar from "../../component/Sidebar";
import { memberSearchFiltersConfig } from "../../constants/memberSearchFiltersConfig";
import { ReferenceConstants } from "../../constants/ReferenceConstants";
import { Member, MemberRequestDto, MemberSearchRequest, MessageType } from "../../constants/types";
import { useNotification } from "../../context/NotificationContext";
import { useReference } from "../../context/ReferenceContext";
import { useApiMessages } from "../../hook/ApiResponseHandler";
import { usePaginatedMembers } from "../../hook/PaginatedMembers";
import { useAuthorization } from "../../hook/useAuthorization";

const { Title } = Typography;

export default function MemberPage() {

    const navigate = useNavigate();
    const notify = useNotification();
    const { members, meta, loading, fetchMembers, setMembers } = usePaginatedMembers();
    const { globalMessages, handleError, resetMessages } = useApiMessages<any>();

    // search filters
    const [filters, setFilters] = useState<MemberSearchRequest>({});

    // modal state
    const [modalOpenInd, setModalOpenInd] = useState(false);
    const [selectedRecord, setSelectedRecord] = useState<Member | null>(null);

    const { canWrite } = useAuthorization();

    const { getReference, toDisplay } = useReference();
    const statusOptions = useMemo(
        () => getReference(ReferenceConstants.MEMBER_STATUS.NAME).map(r => ({ value: r.code, label: r.display })),
        [getReference]
    );

    const handleSearch = async () => {
        resetMessages();
        try {
            await fetchMembers({ ...filters, page: 0, size: meta?.pageSize ?? 10 });
        } catch (e: any) {
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
        const isAdd = !selectedRecord?.memberID;
        const resp = isAdd ? await addMember(values) : await updateMember(values);

        if (resp.globalMessages?.[0]?.type === MessageType.SUCCESS) {
            notify.success({ message: "Success", description: resp.globalMessages[0].message });

            if (isAdd && resp.responseData) {
                // Fetch the newly created member details
                const fullMemberResp = await getMember(resp.responseData);
                if (fullMemberResp.responseData) {
                    // Prepend to the list
                    setMembers(prev => [fullMemberResp.responseData!, ...prev]);
                }
            } else if (!isAdd) {
                // For updates, we still refresh the list to ensure sorting/filtering logic
                fetchMembers(filters);
            }
        }
        setModalOpenInd(false);
        setSelectedRecord(null);
        resetMessages();
    };

    const columns: ColumnsType<Member> = [
        { title: "Member ID", dataIndex: "memberID", key: "memberID", sorter: true, width: 140 },
        { title: "First Name", dataIndex: "firstName", key: "firstName", sorter: true },
        { title: "Last Name", dataIndex: "lastName", key: "lastName", sorter: true },
        { title: "Phone", dataIndex: "phone", key: "phone", width: 120 },
        { title: "Email", dataIndex: "email", key: "email", width: 150 },
        { title: "Status", dataIndex: "status", key: "status", width: 120, render: (code: string) => toDisplay(ReferenceConstants.MEMBER_STATUS.NAME, code) },
        ...(canWrite ? [{
            title: 'Action',
            key: 'action',
            render: (_: any, record: Member) => (
                <Space>
                    <Button icon={<EyeOutlined />} onClick={() => navigate(`/members/${record.memberID}`)} />
                    <Button icon={<EditOutlined />} onClick={() => openEdit(record)} />
                    <Button icon={<DeleteOutlined />} danger onClick={() => handleDelete(record.memberID, `${record.firstName} ${record.lastName}`)} />
                </Space>
            ),
        }] : []),
    ];

    return (
        <div className="dashboard-layout">
            <Sidebar />
            <main className="content fade-in">
                <div style={{ padding: 24 }}>
                    <div className="page-header">
                        <Title level={2} className="page-title">
                            <TeamOutlined /> Members
                        </Title>
                    </div>

                    <SearchFilterBar config={memberSearchFiltersConfig as any} filters={filters} onChange={setFilters} onSearch={handleSearch} onAdd={canWrite ? openAdd : undefined} />

                    {globalMessages && <MessageBanner messages={globalMessages} />}

                    <Card className="glass-card" style={{ padding: 0 }}>
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
                            onChange={(pagination, _filters, sorter) => {
                                const sortField = Array.isArray(sorter) ? sorter[0].field : sorter.field;
                                const sortOrder = Array.isArray(sorter) ? sorter[0].order : sorter.order;

                                resetMessages();
                                fetchMembers({
                                    ...filters, // use the state-level search filters
                                    page: (pagination.current ?? 1) - 1,
                                    size: pagination.pageSize ?? 10,
                                    sortField: sortField as string,
                                    sortOrder: sortOrder === "ascend" ? "asc" : sortOrder === "descend" ? "desc" : undefined,
                                }).catch(handleError);
                            }}
                        />
                    </Card>

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