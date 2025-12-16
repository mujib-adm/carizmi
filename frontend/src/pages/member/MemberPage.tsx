import React, { useMemo, useState } from "react";
import { Table, Button, Space, Input, Select, Typography, Divider } from "antd";
import type { ColumnsType } from "antd/es/table";
import { Link } from "react-router-dom";
import { deleteMember, updateMember, addMember, getMember } from "../../apiclient/memberApi";
import { MessageBanner } from "../../component/MessageBanner";
import { Member, MessageType, MemberRequestDto, MemberSearchParams } from "../../constants/types";
import { usePaginatedMembers } from "../../hook/PaginatedMembers";
import { useNotification } from "../../context/NotificationContext";
import { MemberFormModal } from "../../component/MemberFormModal";

const { Title } = Typography;

export default function MemberPage() {
    const notify = useNotification();

    // hook provides members, meta, loading, globalMessages, fetchMembers
    const { members, meta, loading, globalMessages, fetchMembers, params } = usePaginatedMembers();

    // search filters
    const [filters, setFilters] = useState<MemberSearchParams>({});

    // selection state
    const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([]);
    const selected: Member | null = useMemo(() => {
        const key = selectedRowKeys[0];
        return key ? members.find((m) => m.memberID === key) ?? null : null;
    }, [selectedRowKeys, members]);

    // modal state
    const [modalOpen, setModalOpen] = useState(false);
    const [editInitial, setEditInitial] = useState<Member | null>(null);

    const statusOptions = useMemo(
        () => [
            { value: "Active", label: "Active" },
            { value: "Inactive", label: "Inactive" },
            { value: "Pending", label: "Pending" },
        ],
        []
    );

    const onSearch = () => {
        fetchMembers({ ...filters, page: 0, size: meta?.pageSize ?? 10 });
    };

    const onClear = () => {
        setFilters({});
        setSelectedRowKeys([]);
        fetchMembers({ page: 0, size: meta?.pageSize ?? 10 });
    };

    const openAdd = () => {
        setEditInitial(null);
        setModalOpen(true);
    };

    const openEdit = async () => {
        if (!selected) return;
        try {
            const resp = await getMember(selected.memberID);
            setEditInitial(resp.responseData as Member);
            setModalOpen(true);
        } catch (e) {
            console.error("Error fetching member", e);
        }
    };

    const doDelete = async () => {
        if (!selected) return;
        try {
            const resp = await deleteMember(selected.memberID);
            if (resp.globalMessages?.[0]?.type === MessageType.SUCCESS) {
                notify.success({ message: "Deleted", description: `Member #${selected.memberID} deleted` });
            }
            setSelectedRowKeys([]);
            fetchMembers(filters);
        } catch (e) {
            console.error("Error deleting member", e);
        }
    };

    const submitForm = async (values: MemberRequestDto) => {
        try {
            const payload = editInitial?.memberID ? { ...values, memberID: editInitial.memberID } : values;
            const resp = editInitial?.memberID ? await updateMember(payload) : await addMember(values);

            if (resp.globalMessages?.length > 0) {
                const msg = resp.globalMessages[0];
                if (msg.type === MessageType.SUCCESS) {
                    notify.success({ message: "Success", description: msg.message });
                }
            }
            setModalOpen(false);
            setEditInitial(null);
            setSelectedRowKeys([]);
            fetchMembers(filters);
        } catch (e) {
            console.error("Error submitting member form", e);
            throw e;
        }
    };

    const columns: ColumnsType<Member> = [
        { title: "Member ID", dataIndex: "memberID", key: "memberID", sorter: true, width: 140 },
        { title: "First Name", dataIndex: "firstName", key: "firstName", sorter: true },
        { title: "Last Name", dataIndex: "lastName", key: "lastName", sorter: true },
        { title: "Phone", dataIndex: "phone", key: "phone", width: 120 },
        { title: "Email", dataIndex: "email", key: "email", width: 150 },
        { title: "Status", dataIndex: "status", key: "status", width: 120 },
        {
            title: "Actions",
            key: "actions",
            width: 180,
            render: (_, record) => (
                <Space>
                    <Button onClick={() => { setSelectedRowKeys([record.memberID]); openEdit(); }}>Edit</Button>
                    <Button danger onClick={() => { setSelectedRowKeys([record.memberID]); doDelete(); }}>Delete</Button>
                </Space>
            ),
        },
    ];

    const rowSelection = {
        type: "radio" as const,
        selectedRowKeys,
        onChange: (keys: React.Key[]) => setSelectedRowKeys(keys),
    };

    return (
        <div className="dashboard">
            <nav className="sidebar">
                <ul>
                    <li><Link to="/dashboard">Dashboard</Link></li>
                    <li><Link to="/members">Members</Link></li>
                    <li><Link to="/payments">Payments</Link></li>
                    <li><Link to="/expenses">Expenses</Link></li>
                    <li><Link to="/reporting">Reporting</Link></li>
                    <li><Link to="/settings">System Settings</Link></li>
                    <li><Link to="/logout">Logout</Link></li>
                </ul>
            </nav>

            <main className="content">
                <div className="member-page">
                    <div className="member-header">
                        <Title level={3}>Members</Title>
                        <Space>
                            <Button type="primary" onClick={openAdd}>Add Member</Button>
                            {/* <Button onClick={openEdit} disabled={!selected}>Edit</Button>
                            <Button danger onClick={doDelete} disabled={!selected}>Delete</Button> */}
                        </Space>
                    </div>
                    <Divider style={{ marginTop: 0, marginBottom: "20px" }} />
                    <div className="member-search">
                        <Input
                            placeholder="First Name"
                            value={filters.firstName}
                            onChange={(e) => setFilters({ ...filters, firstName: e.target.value })}
                        />
                        <Input
                            placeholder="Last Name"
                            value={filters.lastName}
                            onChange={(e) => setFilters({ ...filters, lastName: e.target.value })}
                        />
                        <Input
                            placeholder="Phone"
                            value={filters.phone}
                            onChange={(e) => setFilters({ ...filters, phone: e.target.value })}
                        />
                        <Select
                            placeholder="Status"
                            value={filters.status}
                            onChange={(val) => setFilters({ ...filters, status: val })}
                            allowClear
                            style={{ minWidth: 160 }}
                            options={statusOptions}
                        />
                        <Space>
                            <Button type="primary" onClick={onSearch}>Search</Button>
                            <Button onClick={onClear}>Clear</Button>
                        </Space>
                    </div>

                    {globalMessages && <MessageBanner messages={globalMessages} />}

                    <div className="member-grid">
                        <Table<Member>
                            rowKey="memberID"
                            columns={columns}
                            dataSource={members}
                            loading={loading}
                            rowSelection={rowSelection}
                            pagination={{
                                current: meta ? meta.page + 1 : 1,
                                pageSize: meta?.pageSize ?? 10,
                                total: meta?.totalRecords ?? 0,
                                showSizeChanger: true,
                            }}
                            onChange={(pagination, filters, sorter) => {
                                const sortField = Array.isArray(sorter) ? sorter[0].field : sorter.field;
                                const sortOrder = Array.isArray(sorter) ? sorter[0].order : sorter.order;

                                fetchMembers({
                                    ...filters, // your current search filters
                                    page: (pagination.current ?? 1) - 1,
                                    size: pagination.pageSize ?? 10,
                                    sortField: sortField as string,
                                    sortOrder: sortOrder === "ascend" ? "asc" : sortOrder === "descend" ? "desc" : undefined,
                                });
                            }}
                        />
                    </div>

                    <MemberFormModal
                        open={modalOpen}
                        onClose={() => setModalOpen(false)}
                        onSubmit={submitForm}
                        initial={editInitial}
                        statusOptions={statusOptions}
                    />
                </div>
            </main>
        </div>
    );
}